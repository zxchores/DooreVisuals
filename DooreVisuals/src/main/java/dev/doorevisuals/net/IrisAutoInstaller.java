package dev.doorevisuals.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doorevisuals.DooreClient;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.loader.api.FabricLoader;

/** Installs Iris and its exact Modrinth-required dependencies for the next game launch. */
public final class IrisAutoInstaller {
    private static final String API = "https://api.modrinth.com/v2";
    private static final String IRIS = "YL57xq9U";
    private static final String SODIUM = "AANobbMI";
    private static final long MAX_FILE_SIZE = 100L * 1024L * 1024L;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final AtomicBoolean STARTED = new AtomicBoolean();
    private static volatile String status = "Ожидаю запуска автозагрузки Iris…";

    private IrisAutoInstaller() {}

    public static void ensureStarted() {
        if (!STARTED.compareAndSet(false, true)) return;
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            status = "Iris уже установлен и загружен";
            return;
        }
        status = "Проверяю Iris и совместимую Sodium…";
        Thread worker = new Thread(IrisAutoInstaller::install, "DooreVisuals-Iris-Installer");
        worker.setDaemon(true);
        worker.start();
    }

    private static void install() {
        List<Path> staged = new ArrayList<>();
        List<Path> installed = new ArrayList<>();
        try {
            Path mods = FabricLoader.getInstance().getGameDir().resolve("mods").toAbsolutePath().normalize();
            Files.createDirectories(mods);
            if (containsModJar(mods, "iris")) {
                status = "Файл Iris уже есть в mods; перезапусти игру, чтобы он загрузился";
                return;
            }

            status = "Ищу подходящий Iris на Modrinth…";
            JsonObject irisVersion = latestVersion(IRIS);
            List<Artifact> artifacts = new ArrayList<>();
            artifacts.add(artifact(irisVersion));
            JsonArray dependencies = irisVersion.getAsJsonArray("dependencies");
            if (dependencies != null) {
                for (JsonElement entry : dependencies) {
                    JsonObject dep = entry.getAsJsonObject();
                    if (!"required".equals(string(dep, "dependency_type"))) continue;
                    String projectId = string(dep, "project_id");
                    if (!SODIUM.equals(projectId)) throw new IllegalStateException("Iris требует неизвестную зависимость: " + projectId);
                    if (FabricLoader.getInstance().isModLoaded("sodium")) continue;
                    String versionId = string(dep, "version_id");
                    if (versionId.isBlank()) throw new IllegalStateException("У Iris не указан точный совместимый Sodium");
                    artifacts.add(artifact(fetchJson(API + "/version/" + versionId)));
                }
            }

            for (Artifact artifact : artifacts) {
                if (containsFilename(mods, artifact.filename)) {
                    status = "Найден " + artifact.filename + "; перезапусти игру, чтобы загрузить моды";
                    return;
                }
            }
            status = "Скачиваю Iris и его совместимые зависимости…";
            for (Artifact artifact : artifacts) {
                byte[] bytes = download(artifact);
                Path temp = Files.createTempFile(mods, "doore-install-", ".part");
                staged.add(temp);
                Files.write(temp, bytes);
            }
            for (int i = 0; i < artifacts.size(); i++) {
                Path target = mods.resolve(artifacts.get(i).filename).normalize();
                if (!target.getParent().equals(mods)) throw new IllegalStateException("Некорректное имя мода");
                move(staged.get(i), target);
                installed.add(target);
            }
            status = "Iris и Sodium установлены; перезапусти игру для активации";
        } catch (Throwable error) {
            for (Path path : installed) try { Files.deleteIfExists(path); } catch (IOException ignored) {}
            status = "Не удалось установить Iris: " + safe(error);
        } finally {
            for (Path path : staged) try { Files.deleteIfExists(path); } catch (IOException ignored) {}
        }
    }

    private static JsonObject latestVersion(String projectId) throws Exception {
        String uri = API + "/project/" + projectId + "/version?loaders=%5B%22fabric%22%5D&game_versions=%5B%221.21.11%22%5D";
        JsonArray versions = JsonParser.parseString(request(uri)).getAsJsonArray();
        for (JsonElement entry : versions) {
            JsonObject version = entry.getAsJsonObject();
            if ("release".equals(string(version, "version_type")) && compatible(version)) return version;
        }
        throw new IllegalStateException("Не найден стабильный Fabric-релиз для Minecraft 1.21.11");
    }

    private static boolean compatible(JsonObject version) {
        JsonArray loaders = version.getAsJsonArray("loaders"), games = version.getAsJsonArray("game_versions");
        return has(loaders, "fabric") && has(games, "1.21.11");
    }

    private static boolean has(JsonArray array, String value) {
        if (array == null) return false;
        for (JsonElement element : array) if (value.equals(element.getAsString())) return true;
        return false;
    }

    private static Artifact artifact(JsonObject version) {
        JsonArray files = version.getAsJsonArray("files");
        if (files == null || files.isEmpty()) throw new IllegalStateException("У мода нет файла для загрузки");
        JsonObject file = files.get(0).getAsJsonObject();
        for (JsonElement entry : files) if (entry.getAsJsonObject().has("primary") && entry.getAsJsonObject().get("primary").getAsBoolean()) { file = entry.getAsJsonObject(); break; }
        JsonObject hashes = file.getAsJsonObject("hashes");
        if (hashes == null || !hashes.has("sha512")) throw new IllegalStateException("Modrinth не предоставил SHA-512");
        String filename = string(file, "filename");
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\") || !filename.toLowerCase(Locale.ROOT).endsWith(".jar")) throw new IllegalStateException("Некорректное имя файла мода");
        long size = file.get("size").getAsLong();
        if (size <= 0 || size > MAX_FILE_SIZE) throw new IllegalStateException("Размер мода превышает лимит 100 МБ");
        String url = string(file, "url");
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !"cdn.modrinth.com".equalsIgnoreCase(uri.getHost())) throw new IllegalStateException("Неподдерживаемый источник мода");
        return new Artifact(filename, url, hashes.get("sha512").getAsString(), size);
    }

    private static byte[] download(Artifact artifact) throws Exception {
        status = "Скачиваю " + artifact.filename + "…";
        HttpResponse<byte[]> response = HTTP.send(HttpRequest.newBuilder(URI.create(artifact.url)).timeout(Duration.ofSeconds(90)).header("User-Agent", "DooreVisuals/" + DooreClient.VERSION + " (Iris installer)").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) throw new IllegalStateException("CDN ответил кодом " + response.statusCode());
        byte[] bytes = response.body();
        if (bytes.length != artifact.size) throw new IllegalStateException("Размер " + artifact.filename + " не совпал с Modrinth");
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(bytes));
        if (!sha.equalsIgnoreCase(artifact.sha512)) throw new IllegalStateException("Проверка SHA-512 не прошла для " + artifact.filename);
        return bytes;
    }

    private static JsonObject fetchJson(String uri) throws Exception { return JsonParser.parseString(request(uri)).getAsJsonObject(); }

    private static String request(String uri) throws Exception {
        HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(25)).header("User-Agent", "DooreVisuals/" + DooreClient.VERSION + " (Iris installer)").GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("Modrinth ответил кодом " + response.statusCode());
        return response.body();
    }

    private static boolean containsModJar(Path mods, String modId) throws IOException {
        try (var files = Files.list(mods)) { return files.anyMatch(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).startsWith(modId) && path.toString().toLowerCase(Locale.ROOT).endsWith(".jar")); }
    }

    private static boolean containsFilename(Path mods, String filename) throws IOException { return Files.exists(mods.resolve(filename)); }
    private static String string(JsonObject object, String key) { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : ""; }
    private static void move(Path from, Path to) throws IOException { try { Files.move(from, to, StandardCopyOption.ATOMIC_MOVE); } catch (AtomicMoveNotSupportedException ignored) { Files.move(from, to); } }
    private static String safe(Throwable error) { Throwable cause = error; while (cause.getCause() != null) cause = cause.getCause(); return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage(); }
    public static String status() { return status; }

    private record Artifact(String filename, String url, String sha512, long size) {}
}
