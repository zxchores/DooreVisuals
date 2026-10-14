package dev.doorevisuals.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doorevisuals.DooreClient;
import dev.doorevisuals.data.ClientPaths;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Search and on-demand installer for licensed Minecraft shaderpacks on Modrinth. */
public final class IrisShaderCatalog {
    private static final String API = "https://api.modrinth.com/v2";
    private static final long MAX_FILE_SIZE = 150L * 1024L * 1024L;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final AtomicInteger SEARCH_ID = new AtomicInteger();
    private static volatile List<Pack> packs = List.of();
    private static volatile String message = "Введите название или нажмите поиск для популярных шейдеров";
    private static final Map<String, String> jobs = new ConcurrentHashMap<>();

    private IrisShaderCatalog() {}

    public static void search(String query) {
        int requestId = SEARCH_ID.incrementAndGet();
        message = "Загружаю список с Modrinth…";
        String facets = "[[\"project_type:shader\"],[\"versions:1.21.11\"]]";
        String uri = API + "/search?query=" + enc(query == null ? "" : query) + "&facets=" + enc(facets) + "&index=downloads&limit=24";
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(20)).header("User-Agent", "DooreVisuals/" + DooreClient.VERSION + " (shader catalog)").GET().build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            if (requestId != SEARCH_ID.get()) return;
            if (response.statusCode() != 200) { message = "Modrinth ответил кодом " + response.statusCode(); return; }
            try {
                JsonArray hits = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("hits");
                List<Pack> next = new ArrayList<>();
                for (JsonElement element : hits) {
                    JsonObject obj = element.getAsJsonObject();
                    next.add(new Pack(str(obj,"project_id"), str(obj,"title"), str(obj,"description"), str(obj,"author"), str(obj,"license"), obj.has("downloads") ? obj.get("downloads").getAsLong() : 0, str(obj,"slug")));
                }
                if (next.isEmpty()) {
                    packs = List.of();
                    message = "Ничего не найдено для Minecraft 1.21.11";
                } else {
                    packs = List.copyOf(next);
                    List<CompletableFuture<Pack>> details = next.stream().map(IrisShaderCatalog::projectDetails).toList();
                    CompletableFuture.allOf(details.toArray(CompletableFuture[]::new)).thenRun(() -> {
                        if (requestId != SEARCH_ID.get()) return;
                        List<Pack> enriched = details.stream().map(CompletableFuture::join).toList();
                        packs = List.copyOf(enriched);
                        message = "Найдено паков: " + enriched.size() + " · нажмите «Скачать»";
                    });
                    message = "Найдено паков: " + next.size() + " · загружаю авторов и лицензии…";
                }
            } catch (Throwable error) { message = "Не удалось разобрать ответ Modrinth"; }
        }).exceptionally(error -> { if (requestId == SEARCH_ID.get()) message = "Ошибка сети: " + safe(error); return null; });
    }

    public static void install(Pack pack) {
        if (pack == null) return;
        String previous = jobs.putIfAbsent(pack.id(), "Ищу совместимую версию…");
        if (previous != null) {
            if (!previous.startsWith("Ошибка") && !previous.startsWith("Установлен")) return;
            jobs.put(pack.id(), "Ищу совместимую версию…");
        }
        String uri = API + "/project/" + enc(pack.id()) + "/version?game_versions=" + enc("[\"1.21.11\"]");
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(20)).header("User-Agent", "DooreVisuals/" + DooreClient.VERSION + " (shader catalog)").GET().build();
        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenCompose(response -> {
            if (response.statusCode() != 200) throw new IllegalStateException("Modrinth version lookup: " + response.statusCode());
            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            JsonObject file = primary(versions);
            long size = file.get("size").getAsLong();
            if (size <= 0 || size > MAX_FILE_SIZE) throw new IllegalStateException("Размер пакета превышает лимит 150 МБ");
            String url = file.get("url").getAsString();
            URI fileUri = URI.create(url);
            if (!"https".equalsIgnoreCase(fileUri.getScheme()) || !"cdn.modrinth.com".equalsIgnoreCase(fileUri.getHost())) throw new IllegalStateException("Неподдерживаемый источник файла");
            String filename = file.get("filename").getAsString();
            String hash = file.getAsJsonObject("hashes").get("sha512").getAsString();
            jobs.put(pack.id(), "Скачиваю…");
            HttpRequest download = HttpRequest.newBuilder(fileUri).timeout(Duration.ofSeconds(90)).header("User-Agent", "DooreVisuals/" + DooreClient.VERSION + " (shader catalog)").GET().build();
            return HTTP.sendAsync(download, HttpResponse.BodyHandlers.ofByteArray()).thenApply(result -> {
                if (result.statusCode() != 200) throw new IllegalStateException("Загрузка файла: " + result.statusCode());
                byte[] bytes = result.body();
                if (bytes.length != size || bytes.length > MAX_FILE_SIZE) throw new IllegalStateException("Размер файла не совпадает с каталогом");
                try {
                    String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(bytes));
                    if (!actual.equalsIgnoreCase(hash)) throw new IllegalStateException("SHA-512 не совпал; файл удалён");
                    Path dir = FabricGamePaths.shaderpacks();
                    Files.createDirectories(dir);
                    String safe = filename.replaceAll("[^A-Za-z0-9._ -]", "_");
                    if (!safe.toLowerCase().endsWith(".zip")) safe += ".zip";
                    Path destination = unique(dir, safe);
                    Path temp = Files.createTempFile(dir, "doore-shader-", ".part");
                    try {
                        Files.write(temp, bytes);
                        try { Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE); }
                        catch (AtomicMoveNotSupportedException ignored) { Files.move(temp, destination); }
                    } finally { Files.deleteIfExists(temp); }
                    return destination;
                } catch (IllegalStateException error) { throw error; }
                catch (Exception error) { throw new IllegalStateException("Не удалось сохранить shaderpack", error); }
            });
        }).thenAccept(path -> jobs.put(pack.id(), "Установлен: " + path.getFileName()))
          .exceptionally(error -> { jobs.put(pack.id(), "Ошибка: " + safe(error)); return null; });
    }

    private static CompletableFuture<Pack> projectDetails(Pack fallback) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + "/project/" + enc(fallback.id())))
            .timeout(Duration.ofSeconds(15)).header("User-Agent", "DooreVisuals/" + DooreClient.VERSION + " (shader catalog)").GET().build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) return fallback;
            try {
                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                String license = obj.has("license") && obj.get("license").isJsonObject() ? str(obj.getAsJsonObject("license"), "id") : "";
                return new Pack(fallback.id(), fallback.title(), fallback.description(), fallback.author(), license, fallback.downloads(), fallback.slug());
            } catch (RuntimeException ignored) { return fallback; }
        }).exceptionally(error -> fallback);
    }

    private static JsonObject primary(JsonArray versions) {
        for (JsonElement entry : versions) {
            JsonArray files = entry.getAsJsonObject().getAsJsonArray("files");
            for (JsonElement file : files) if (file.getAsJsonObject().has("primary") && file.getAsJsonObject().get("primary").getAsBoolean()) return file.getAsJsonObject();
            if (!files.isEmpty()) return files.get(0).getAsJsonObject();
        }
        throw new IllegalStateException("Нет совместимого файла для Minecraft 1.21.11");
    }

    private static Path unique(Path dir, String name) {
        Path target = dir.resolve(name).normalize();
        if (!target.getParent().equals(dir.normalize())) throw new IllegalStateException("Некорректное имя файла");
        if (!Files.exists(target)) return target;
        String stem = name.endsWith(".zip") ? name.substring(0, name.length() - 4) : name;
        for (int i = 2; i < 10000; i++) { Path candidate = dir.resolve(stem + " (" + i + ").zip"); if (!Files.exists(candidate)) return candidate; }
        throw new IllegalStateException("Слишком много файлов с одинаковым именем");
    }

    public static List<Pack> packs() { return packs; }
    public static String message() { return message; }
    public static String job(String id) { return jobs.getOrDefault(id, ""); }
    private static String str(JsonObject obj, String key) { return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : ""; }
    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String safe(Throwable error) { Throwable t=error; while(t.getCause()!=null)t=t.getCause(); return t.getMessage()==null ? t.getClass().getSimpleName() : t.getMessage(); }

    public record Pack(String id, String title, String description, String author, String license, long downloads, String slug) {}

    private static final class FabricGamePaths {
        static Path shaderpacks() { return net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("shaderpacks"); }
    }
}
