package dev.doorelauncher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/** Downloads Minecraft, Fabric and Java from their official servers into the launcher's own folder, and starts the game. */
final class Game {
    static final String MC = "1.21.11";
    static final String LOADER = "0.19.3";
    static final String FABRIC_ID = "fabric-loader-" + LOADER + "-" + MC;
    static final Path HOME = Path.of(System.getenv("APPDATA") == null ? System.getProperty("user.home") : System.getenv("APPDATA"), ".doorelauncher");

    interface Progress {
        void status(String text);

        void fraction(double value);
    }

    /** Everything needed to start: the class path, the arguments with their rules still in, the Java to use. */
    record Plan(String mainClass, String assetIndex, List<Path> classpath, List<Object> jvm, List<Object> game, Path java) {
    }

    private record Job(String url, Path path, String sha1, long size) {
        boolean present() throws IOException {
            return Files.isRegularFile(path) && (size <= 0 || Files.size(path) == size);
        }
    }

    private Game() {
    }

    static Plan install(Progress p) throws Exception {
        Files.createDirectories(HOME);
        p.status("Проверка версии…");
        Map<String, Object> vanilla = vanilla();
        Map<String, Object> fabric = fabric();

        List<Job> jobs = new ArrayList<>();
        List<Path> classpath = new ArrayList<>();
        Map<String, Path> libs = new LinkedHashMap<>();
        for (Object o : Json.list(vanilla.get("libraries"))) {
            Map<String, Object> lib = Json.obj(o);
            if (!allowed(lib.get("rules"))) {
                continue;
            }
            Map<String, Object> art = Json.obj(Json.obj(lib.get("downloads")).get("artifact"));
            if (art.isEmpty()) {
                continue;
            }
            Path file = HOME.resolve("libraries").resolve(Json.str(art.get("path")));
            jobs.add(new Job(Json.str(art.get("url")), file, Json.str(art.get("sha1")), Json.num(art.get("size"))));
            libs.put(key(Json.str(lib.get("name"))), file);
        }
        for (Object o : Json.list(fabric.get("libraries"))) {
            Map<String, Object> lib = Json.obj(o);
            String name = Json.str(lib.get("name"));
            String[] c = name.split(":");
            String rel = c[0].replace('.', '/') + "/" + c[1] + "/" + c[2] + "/" + c[1] + "-" + c[2] + (c.length > 3 ? "-" + c[3] : "") + ".jar";
            Path file = HOME.resolve("libraries").resolve(rel);
            jobs.add(new Job(Json.str(lib.get("url")) + rel, file, Json.str(lib.get("sha1")), Json.num(lib.get("size"))));
            libs.put(key(name), file);
        }
        classpath.addAll(libs.values());

        Path clientJar = HOME.resolve("versions").resolve(MC).resolve(MC + ".jar");
        Map<String, Object> client = Json.obj(Json.obj(vanilla.get("downloads")).get("client"));
        jobs.add(new Job(Json.str(client.get("url")), clientJar, Json.str(client.get("sha1")), Json.num(client.get("size"))));
        classpath.add(clientJar);

        Map<String, Object> index = Json.obj(vanilla.get("assetIndex"));
        String indexId = Json.str(index.get("id"));
        Path indexFile = HOME.resolve("assets/indexes").resolve(indexId + ".json");
        if (!Files.isRegularFile(indexFile)) {
            Http.download(Json.str(index.get("url")), indexFile, Json.str(index.get("sha1")));
        }
        Map<String, Object> objects = Json.obj(Json.obj(Json.parse(Files.readString(indexFile))).get("objects"));
        for (Object o : objects.values()) {
            Map<String, Object> obj = Json.obj(o);
            String hash = Json.str(obj.get("hash"));
            String dir = hash.substring(0, 2);
            jobs.add(new Job("https://resources.download.minecraft.net/" + dir + "/" + hash,
                HOME.resolve("assets/objects").resolve(dir).resolve(hash), hash, Json.num(obj.get("size"))));
        }

        String component = Json.str(Json.obj(vanilla.get("javaVersion")).get("component"));
        Path runtimeDir = HOME.resolve("runtime").resolve(component);
        Path marker = runtimeDir.resolve(".complete");
        if (!Files.exists(marker)) {
            p.status("Список файлов Java…");
            Map<String, Object> all = Json.obj(Json.parse(Http.text(
                "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json")));
            Map<String, Object> entry = Json.obj(Json.list(Json.obj(all.get("windows-x64")).get(component)).get(0));
            Map<String, Object> manifest = Json.obj(Json.parse(Http.text(Json.str(Json.obj(entry.get("manifest")).get("url")))));
            for (Map.Entry<String, Object> e : Json.obj(manifest.get("files")).entrySet()) {
                Map<String, Object> f = Json.obj(e.getValue());
                if (!"file".equals(Json.str(f.get("type")))) {
                    continue;
                }
                Map<String, Object> raw = Json.obj(Json.obj(f.get("downloads")).get("raw"));
                jobs.add(new Job(Json.str(raw.get("url")), runtimeDir.resolve(e.getKey()), Json.str(raw.get("sha1")), Json.num(raw.get("size"))));
            }
        }

        fetchAll(jobs, p);
        Files.createDirectories(runtimeDir);
        Files.writeString(marker, "ok");

        List<Object> jvm = new ArrayList<>(Json.list(Json.obj(vanilla.get("arguments")).get("jvm")));
        jvm.addAll(Json.list(Json.obj(fabric.get("arguments")).get("jvm")));
        List<Object> game = new ArrayList<>(Json.list(Json.obj(vanilla.get("arguments")).get("game")));
        game.addAll(Json.list(Json.obj(fabric.get("arguments")).get("game")));
        return new Plan(Json.str(fabric.get("mainClass")), indexId, classpath, jvm, game, runtimeDir.resolve("bin").resolve("javaw.exe"));
    }

    private static String key(String name) {
        String[] c = name.split(":");
        return c[0] + ":" + c[1] + (c.length > 3 ? ":" + c[3] : "");
    }

    private static Map<String, Object> vanilla() throws IOException {
        Path file = HOME.resolve("versions").resolve(MC).resolve(MC + ".json");
        if (!Files.isRegularFile(file)) {
            Map<String, Object> manifest = Json.obj(Json.parse(Http.text("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")));
            for (Object o : Json.list(manifest.get("versions"))) {
                Map<String, Object> v = Json.obj(o);
                if (MC.equals(Json.str(v.get("id")))) {
                    Http.download(Json.str(v.get("url")), file, Json.str(v.get("sha1")));
                    break;
                }
            }
        }
        if (!Files.isRegularFile(file)) {
            throw new IOException("Не нашёл Minecraft " + MC + " у Mojang.");
        }
        return Json.obj(Json.parse(Files.readString(file)));
    }

    private static Map<String, Object> fabric() throws IOException {
        Path file = HOME.resolve("versions").resolve(FABRIC_ID).resolve(FABRIC_ID + ".json");
        if (!Files.isRegularFile(file)) {
            Http.download("https://meta.fabricmc.net/v2/versions/loader/" + MC + "/" + LOADER + "/profile/json", file, null);
        }
        return Json.obj(Json.parse(Files.readString(file)));
    }

    private static void fetchAll(List<Job> jobs, Progress p) throws Exception {
        List<Job> missing = new ArrayList<>();
        for (Job j : jobs) {
            if (!j.present()) {
                missing.add(j);
            }
        }
        if (missing.isEmpty()) {
            p.fraction(1.0);
            return;
        }
        AtomicInteger done = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(12);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Job j : missing) {
                futures.add(pool.submit(() -> {
                    Http.download(j.url(), j.path(), j.sha1());
                    int n = done.incrementAndGet();
                    p.fraction(n / (double)missing.size());
                    p.status("Загрузка игры: " + n + " / " + missing.size());
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /** Whether the rules (os and features) of a library or argument let it in on this machine. */
    static boolean allowed(Object rules) {
        List<Object> list = Json.list(rules);
        if (list.isEmpty()) {
            return true;
        }
        boolean result = false;
        for (Object o : list) {
            Map<String, Object> rule = Json.obj(o);
            boolean matches = true;
            Map<String, Object> os = Json.obj(rule.get("os"));
            if (!os.isEmpty()) {
                String name = Json.str(os.get("name"));
                String arch = Json.str(os.get("arch"));
                matches = (name.isEmpty() || name.equals("windows")) && (arch.isEmpty() || !arch.equals("x86"));
            }
            if (!Json.obj(rule.get("features")).isEmpty()) {
                matches = false;
            }
            if (matches) {
                result = "allow".equals(Json.str(rule.get("action")));
            }
        }
        return result;
    }

    private static void expand(List<Object> args, Map<String, String> vars, List<String> out) {
        for (Object a : args) {
            if (a instanceof String s) {
                out.add(fill(s, vars));
            } else if (a instanceof Map<?, ?> m) {
                Map<String, Object> arg = Json.obj(m);
                if (!allowed(arg.get("rules"))) {
                    continue;
                }
                Object value = arg.get("value");
                if (value instanceof String s) {
                    out.add(fill(s, vars));
                } else {
                    for (Object v : Json.list(value)) {
                        out.add(fill(Json.str(v), vars));
                    }
                }
            }
        }
    }

    private static String fill(String s, Map<String, String> vars) {
        for (Map.Entry<String, String> e : vars.entrySet()) {
            s = s.replace("${" + e.getKey() + "}", e.getValue());
        }
        return s.replaceAll("\\$\\{[a-z_]+}", "");
    }

    /** Starts the game; its output is merged into one stream the caller reads. extraJvm: more JVM arguments, server: host to join. */
    static Process start(Plan plan, Auth.Session session, int memoryGb, String clientId, String extraJvm, String server) throws IOException {
        Path gameDir = HOME.resolve("game");
        Path natives = HOME.resolve("versions").resolve(FABRIC_ID).resolve("natives");
        Files.createDirectories(gameDir);
        Files.createDirectories(natives);
        StringBuilder cp = new StringBuilder();
        for (Path path : plan.classpath()) {
            if (cp.length() > 0) {
                cp.append(';');
            }
            cp.append(path.toString());
        }
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("auth_player_name", session.name());
        vars.put("version_name", FABRIC_ID);
        vars.put("game_directory", gameDir.toString());
        vars.put("assets_root", HOME.resolve("assets").toString());
        vars.put("assets_index_name", plan.assetIndex());
        vars.put("auth_uuid", session.uuid());
        vars.put("auth_access_token", session.accessToken());
        vars.put("clientid", clientId);
        vars.put("auth_xuid", "0");
        vars.put("version_type", "release");
        vars.put("launcher_name", "DooreLauncher");
        vars.put("launcher_version", "1.0");
        vars.put("natives_directory", natives.toString());
        vars.put("classpath", cp.toString());
        vars.put("classpath_separator", ";");
        vars.put("library_directory", HOME.resolve("libraries").toString());

        List<String> command = new ArrayList<>();
        command.add(plan.java().toString());
        command.add("-Xmx" + memoryGb + "G");
        command.add("-Xms512M");
        for (String arg : extraJvm.trim().split("\\s+")) {
            if (!arg.isEmpty()) {
                command.add(arg);
            }
        }
        expand(plan.jvm(), vars, command);
        command.add(plan.mainClass());
        expand(plan.game(), vars, command);
        if (server != null && !server.isBlank()) {
            command.add("--quickPlayMultiplayer");
            command.add(server.trim());
        }
        return new ProcessBuilder(command).directory(gameDir.toFile()).redirectErrorStream(true).start();
    }
}
