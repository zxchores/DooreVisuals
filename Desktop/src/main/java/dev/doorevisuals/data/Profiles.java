package dev.doorevisuals.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Profiles {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Profiles");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path dir;
    private final Path state;
    private final Supplier<JsonObject> snapshot;
    private final Consumer<JsonObject> apply;
    private String active = "default";
    private String defaultProfile = "default";
    private String lastError = "";
    private final Map<String, String> servers = new LinkedHashMap<>();
    private final Map<String, String> skip = new LinkedHashMap<>();

    public Profiles(Supplier<JsonObject> snapshot, Consumer<JsonObject> apply) {
        this.snapshot = snapshot;
        this.apply = apply;
        this.dir = ClientPaths.configs();
        this.state = ClientPaths.state();
        this.migrateLegacy();
    }

    public Path dir() {
        return this.dir;
    }

    public String active() {
        return this.active;
    }

    public String defaultProfile() {
        return this.defaultProfile;
    }

    public String lastError() {
        return this.lastError;
    }

    public void boot() {
        try {
            Files.createDirectories(this.dir);
        } catch (IOException ioexception) {
            LOG.warn("Cannot create configs dir", ioexception);
        }

        this.migrateFoldersToCfg();
        this.migrateFlatJson();
        this.loadState();
        Path path = this.file(this.active);
        if (!Files.exists(path)) {
            this.saveAs(this.active);
        } else {
            this.load(this.active);
        }
    }

    public List<String> list() {
        List<String> list = new ArrayList<>();
        if (!Files.isDirectory(this.dir)) {
            return list;
        } else {
            try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(this.dir, "*.cfg")) {
                for (Path path : directorystream) {
                    if (Files.isRegularFile(path)) {
                        String s = path.getFileName().toString();
                        String s1 = clean(s.substring(0, s.length() - 4));
                        if (!s1.isEmpty() && !list.contains(s1)) {
                            list.add(s1);
                        }
                    }
                }
            } catch (IOException ioexception) {
                LOG.warn("List configs failed", ioexception);
            }

            list.sort(Comparator.naturalOrder());
            return list;
        }
    }

    public boolean saveAs(String name) {
        return this.saveAs(name, "", "");
    }

    public boolean saveAs(String name, String note, String tags) {
        String s = clean(name);
        if (s.isEmpty()) {
            this.lastError = "\u043f\u0443\u0441\u0442\u043e\u0435 \u0438\u043c\u044f";
            return false;
        } else {
            try {
                JsonObject jsonobject = this.snapshot.get();
                this.attachMetadata(s, jsonobject, note, tags);
                this.writeFile(s, jsonobject);
                this.active = s;
                this.writeState();
                this.lastError = "";
                return true;
            } catch (Exception exception) {
                this.lastError = exception.getMessage() == null
                    ? "\u043e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u043f\u0438\u0441\u0438"
                    : exception.getMessage();
                LOG.error("saveAs({}) failed", s, exception);
                return false;
            }
        }
    }

    public Profiles.Metadata metadata(String name) {
        try {
            Path path = this.file(clean(name));
            if (!Files.exists(path)) {
                return new Profiles.Metadata(0L, 0L, "", "", "");
            } else {
                JsonObject jsonobject = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject jsonobject1 = jsonobject.has("_meta") && jsonobject.get("_meta").isJsonObject()
                    ? jsonobject.getAsJsonObject("_meta")
                    : new JsonObject();
                return new Profiles.Metadata(
                    jsonobject1.has("created_at") ? jsonobject1.get("created_at").getAsLong() : Files.getLastModifiedTime(path).toMillis(),
                    jsonobject1.has("updated_at") ? jsonobject1.get("updated_at").getAsLong() : Files.getLastModifiedTime(path).toMillis(),
                    jsonobject1.has("note") ? jsonobject1.get("note").getAsString() : "",
                    jsonobject1.has("tags") ? jsonobject1.get("tags").getAsString() : "",
                    jsonobject1.has("author") ? jsonobject1.get("author").getAsString() : "",
                    jsonobject1.has("server") ? jsonobject1.get("server").getAsString() : ""
                );
            }
        } catch (Exception exception) {
            return new Profiles.Metadata(0L, 0L, "", "", "");
        }
    }

    public boolean setDefault(String name) {
        String s = clean(name);
        if (!Files.exists(this.file(s))) {
            this.lastError = "\u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + s;
            return false;
        } else {
            this.defaultProfile = s;
            this.writeState();
            return true;
        }
    }

    public void writeActive(JsonObject snap) throws IOException {
        if (snap != null) {
            this.writeFile(this.active, snap);
            this.writeState();
        }
    }

    public boolean load(String name) {
        String s = clean(name);
        Path path = this.file(s);
        if (!Files.exists(path)) {
            this.lastError = "\u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + s;
            return false;
        } else {
            try {
                if (!this.active.isBlank() && Files.exists(this.file(this.active))) {
                    this.backupBeforeApply(this.active);
                }

                JsonObject jsonobject = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
                this.active = s;
                this.writeState();
                this.apply.accept(jsonobject);
                this.lastError = "";
                return true;
            } catch (Exception exception) {
                this.lastError = exception.getMessage() == null
                    ? "\u043e\u0448\u0438\u0431\u043a\u0430 \u0447\u0442\u0435\u043d\u0438\u044f"
                    : exception.getMessage();
                LOG.error("load({}) failed", s, exception);
                return false;
            }
        }
    }

    public boolean delete(String name) {
        String s = clean(name);
        if (s.equals(this.active)) {
            this.lastError = "\u0430\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u043a\u043e\u043d\u0444\u0438\u0433 \u043d\u0435\u043b\u044c\u0437\u044f \u0443\u0434\u0430\u043b\u0438\u0442\u044c";
            return false;
        } else if (s.equals("default")) {
            this.lastError = "default \u043d\u0435\u043b\u044c\u0437\u044f \u0443\u0434\u0430\u043b\u0438\u0442\u044c";
            return false;
        } else {
            try {
                Files.deleteIfExists(this.file(s));
                Path path = this.dir.resolve(s);
                if (Files.isDirectory(path)) {
                    deleteTree(path);
                }

                this.writeState();
                this.lastError = "";
                return true;
            } catch (IOException ioexception) {
                this.lastError = ioexception.getMessage() == null
                    ? "\u043e\u0448\u0438\u0431\u043a\u0430 \u0443\u0434\u0430\u043b\u0435\u043d\u0438\u044f"
                    : ioexception.getMessage();
                LOG.error("delete({}) failed", s, ioexception);
                return false;
            }
        }
    }

    public boolean rename(String from, String to) {
        String s = clean(from);
        String s1 = clean(to);
        if (s.isEmpty() || s1.isEmpty()) {
            this.lastError = "\u043f\u0443\u0441\u0442\u043e\u0435 \u0438\u043c\u044f";
            return false;
        } else if (s.equals("default")) {
            this.lastError = "default \u043d\u0435\u043b\u044c\u0437\u044f \u043f\u0435\u0440\u0435\u0438\u043c\u0435\u043d\u043e\u0432\u0430\u0442\u044c";
            return false;
        } else {
            Path path = this.file(s);
            Path path1 = this.file(s1);
            if (!Files.exists(path)) {
                this.lastError = "\u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + s;
                return false;
            } else if (Files.exists(path1)) {
                this.lastError = "\u0443\u0436\u0435 \u0435\u0441\u0442\u044c: " + s1;
                return false;
            } else {
                try {
                    Files.move(path, path1);
                    if (this.active.equals(s)) {
                        this.active = s1;
                        this.writeState();
                    }

                    this.lastError = "";
                    return true;
                } catch (IOException ioexception) {
                    this.lastError = ioexception.getMessage() == null ? "\u043e\u0448\u0438\u0431\u043a\u0430 rename" : ioexception.getMessage();
                    return false;
                }
            }
        }
    }

    public boolean duplicate(String from, String to) {
        String s = clean(from);
        String s1 = clean(to);
        if (!s.isEmpty() && !s1.isEmpty()) {
            if (!Files.exists(this.file(s))) {
                this.lastError = "\u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + s;
                return false;
            } else if (Files.exists(this.file(s1))) {
                this.lastError = "\u0443\u0436\u0435 \u0435\u0441\u0442\u044c: " + s1;
                return false;
            } else {
                try {
                    Files.copy(this.file(s), this.file(s1));
                    this.lastError = "";
                    return true;
                } catch (IOException ioexception) {
                    this.lastError = ioexception.getMessage() == null ? "\u043e\u0448\u0438\u0431\u043a\u0430 copy" : ioexception.getMessage();
                    return false;
                }
            }
        } else {
            this.lastError = "\u043f\u0443\u0441\u0442\u043e\u0435 \u0438\u043c\u044f";
            return false;
        }
    }

    public void backupBeforeApply(String name) {
        try {
            Path path = this.file(clean(name));
            if (!Files.exists(path)) {
                return;
            }

            Path path1 = this.dir.resolve("backups");
            Files.createDirectories(path1);
            String s = Long.toString(System.currentTimeMillis());
            Files.copy(path, path1.resolve(clean(name) + "-" + s + ".cfg"), StandardCopyOption.REPLACE_EXISTING);
            List<Path> list = new ArrayList<>();

            try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(path1, "*.cfg")) {
                for (Path path2 : directorystream) {
                    list.add(path2);
                }
            }

            try (DirectoryStream<Path> directorystream1 = Files.newDirectoryStream(path1, "*.json")) {
                for (Path path3 : directorystream1) {
                    list.add(path3);
                }
            }

            list.sort(Comparator.<Path, String>comparing(px -> px.getFileName().toString()).reversed());

            for (int i = 8; i < list.size(); i++) {
                Files.deleteIfExists(list.get(i));
            }
        } catch (Exception exception) {
            LOG.warn("backupBeforeApply failed", exception);
        }
    }

    public void autosave() {
        this.saveAs(this.active);
    }

    public boolean openFolder() {
        return Folders.open(this.dir);
    }

    public boolean openFolder(String name) {
        return this.openFolder();
    }

    public Path folderOf(String name) {
        return this.dir;
    }

    public Path fileOf(String name) {
        return this.file(clean(name));
    }

    private void writeFile(String name, JsonObject snap) throws IOException {
        Files.createDirectories(this.dir);
        Path path = this.file(name);
        Path path1 = this.dir.resolve(name + ".cfg.tmp");
        Files.writeString(path1, GSON.toJson(snap), StandardCharsets.UTF_8);

        try {
            Files.move(path1, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ioexception) {
            Files.move(path1, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void attachMetadata(String name, JsonObject snap, String note, String tags) {
        long i = System.currentTimeMillis();
        Profiles.Metadata profiles$metadata = this.metadata(name);
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("created_at", profiles$metadata.createdAt() > 0L ? profiles$metadata.createdAt() : i);
        jsonobject.addProperty("updated_at", i);
        jsonobject.addProperty("note", note != null && !note.isBlank() ? note.trim() : profiles$metadata.note());
        jsonobject.addProperty("tags", tags != null && !tags.isBlank() ? tags.trim() : profiles$metadata.tags());
        String s = profiles$metadata.author();
        if (s == null || s.isBlank()) {
            s = sessionNick();
        }

        jsonobject.addProperty("author", s);
        String s1 = profiles$metadata.server();
        String s2 = currentServerKey();
        if (!s2.isEmpty()) {
            s1 = s2;
        }

        jsonobject.addProperty("server", s1 == null ? "" : s1);
        snap.add("_meta", jsonobject);
    }

    public static String currentServerKey() {
        try {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient != null && minecraftclient.getCurrentServerEntry() != null && minecraftclient.getCurrentServerEntry().address != null) {
                return normalizeServer(minecraftclient.getCurrentServerEntry().address);
            }
        } catch (Exception exception) {
        }

        return "";
    }

    public static String normalizeServer(String ip) {
        return ip == null ? "" : ip.trim().toLowerCase(Locale.ROOT);
    }

    public String suggestForServer(String ip) {
        String s = normalizeServer(ip);
        if (s.isEmpty()) {
            return null;
        } else {
            String s1 = this.servers.get(s);
            if (s1 != null && Files.exists(this.file(clean(s1)))) {
                return clean(s1);
            } else {
                String s2 = clean(s.replace(':', '-').replace('.', '-'));
                if (!s2.isEmpty() && Files.exists(this.file(s2))) {
                    return s2;
                } else {
                    for (String s3 : this.list()) {
                        Profiles.Metadata profiles$metadata = this.metadata(s3);
                        if (s.equalsIgnoreCase(normalizeServer(profiles$metadata.server()))) {
                            return s3;
                        }
                    }

                    return null;
                }
            }
        }
    }

    public boolean skipped(String ip, String cfg) {
        String s = normalizeServer(ip);
        String s1 = clean(cfg);
        return !s1.isEmpty() && s1.equalsIgnoreCase(this.skip.getOrDefault(s, ""));
    }

    public void skipPrompt(String ip, String cfg) {
        String s = normalizeServer(ip);
        String s1 = clean(cfg);
        if (!s.isEmpty() && !s1.isEmpty()) {
            this.skip.put(s, s1);
            this.writeState();
        }
    }

    public void bindServer(String ip, String cfg) {
        String s = normalizeServer(ip);
        String s1 = clean(cfg);
        if (!s.isEmpty() && !s1.isEmpty()) {
            this.servers.put(s, s1);
            this.skip.remove(s);
            this.writeState();
        }
    }

    static String sessionNick() {
        try {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient != null && minecraftclient.getSession() != null && minecraftclient.getSession().getUsername() != null) {
                return minecraftclient.getSession().getUsername();
            }
        } catch (Exception exception) {
        }

        return "player";
    }

    private Path file(String name) {
        return this.dir.resolve(name + ".cfg");
    }

    private void migrateFoldersToCfg() {
        if (Files.isDirectory(this.dir)) {
            try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(this.dir)) {
                for (Path path : directorystream) {
                    if (Files.isDirectory(path)) {
                        String s = path.getFileName().toString();
                        if (!"backups".equals(s) && !s.startsWith(".")) {
                            Path path1 = path.resolve("config.json");
                            if (Files.isRegularFile(path1)) {
                                String s1 = clean(s);
                                if (!s1.isEmpty()) {
                                    Path path2 = this.file(s1);
                                    if (!Files.exists(path2)) {
                                        Files.move(path1, path2);
                                    }

                                    deleteTree(path);
                                }
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                LOG.warn("migrateFoldersToCfg failed", exception);
            }
        }
    }

    private void migrateFlatJson() {
        if (Files.isDirectory(this.dir)) {
            try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(this.dir, "*.json")) {
                for (Path path : directorystream) {
                    String s = path.getFileName().toString();
                    String s1 = clean(s.substring(0, s.length() - 5));
                    if (!s1.isEmpty()) {
                        Path path1 = this.file(s1);
                        if (!Files.exists(path1)) {
                            Files.move(path, path1);
                        } else {
                            Files.deleteIfExists(path);
                        }
                    }
                }
            } catch (Exception exception) {
                LOG.warn("migrateFlatJson failed", exception);
            }
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (Files.exists(root)) {
            try (Stream<Path> stream = Files.walk(root)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ioexception) {
                    }
                });
            }
        }
    }

    private void loadState() {
        if (Files.exists(this.state)) {
            try {
                JsonObject jsonobject = JsonParser.parseString(Files.readString(this.state, StandardCharsets.UTF_8)).getAsJsonObject();
                if (jsonobject.has("profile")) {
                    this.active = clean(jsonobject.get("profile").getAsString());
                    if (this.active.isEmpty()) {
                        this.active = "default";
                    }
                }

                if (jsonobject.has("default_profile")) {
                    String s = clean(jsonobject.get("default_profile").getAsString());
                    if (!s.isEmpty()) {
                        this.defaultProfile = s;
                    }
                }

                readMap(jsonobject, "servers", this.servers);
                readMap(jsonobject, "skip", this.skip);
            } catch (Exception exception) {
                LOG.warn("loadState failed", exception);
            }
        }
    }

    private void writeState() {
        try {
            Files.createDirectories(ClientPaths.root());
            JsonObject jsonobject = new JsonObject();
            if (Files.exists(this.state)) {
                try {
                    jsonobject = JsonParser.parseString(Files.readString(this.state, StandardCharsets.UTF_8)).getAsJsonObject();
                } catch (Exception exception) {
                }
            }

            jsonobject.addProperty("profile", this.active);
            jsonobject.addProperty("default_profile", this.defaultProfile);
            jsonobject.add("servers", toJson(this.servers));
            jsonobject.add("skip", toJson(this.skip));
            Files.writeString(this.state, GSON.toJson(jsonobject), StandardCharsets.UTF_8);
        } catch (IOException ioexception) {
            LOG.warn("writeState failed", ioexception);
        }
    }

    private void migrateLegacy() {
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("doorevisuals").resolve("profiles");
            if (!Files.isDirectory(path)) {
                return;
            }

            Files.createDirectories(this.dir);

            try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(path, "*.json")) {
                for (Path path1 : directorystream) {
                    Path path2 = this.dir.resolve(path1.getFileName());
                    if (!Files.exists(path2)) {
                        Files.copy(path1, path2);
                    }
                }
            }

            Path path3 = path.getParent().resolve("state.json");
            if (Files.exists(path3) && !Files.exists(this.state)) {
                Files.copy(path3, this.state);
            }
        } catch (Exception exception) {
            LOG.warn("migrateLegacy failed", exception);
        }
    }

    public static String clean(String name) {
        if (name == null) {
            return "";
        } else {
            String s = name.trim().toLowerCase(Locale.ROOT);
            if (s.endsWith(".cfg")) {
                s = s.substring(0, s.length() - 4);
            }

            if (s.endsWith(".json")) {
                s = s.substring(0, s.length() - 5);
            }

            s = s.replaceAll("[^a-z0-9._-]+", "-");

            while (s.startsWith("-") || s.startsWith(".")) {
                s = s.substring(1);
            }

            return s.length() > 48 ? s.substring(0, 48) : s;
        }
    }

    private static void readMap(JsonObject o, String key, Map<String, String> into) {
        into.clear();
        if (o.has(key) && o.get(key).isJsonObject()) {
            JsonObject jsonobject = o.getAsJsonObject(key);

            for (String s : jsonobject.keySet()) {
                try {
                    String s1 = clean(jsonobject.get(s).getAsString());
                    if (!s.isBlank() && !s1.isEmpty()) {
                        into.put(normalizeServer(s), s1);
                    }
                } catch (Exception exception) {
                }
            }
        }
    }

    private static JsonObject toJson(Map<String, String> map) {
        JsonObject jsonobject = new JsonObject();

        for (Entry<String, String> entry : map.entrySet()) {
            jsonobject.addProperty(entry.getKey(), entry.getValue());
        }

        return jsonobject;
    }

    public static String fileLabel(String name) {
        String s = clean(name);
        return s.isEmpty() ? "" : s + ".cfg";
    }

    public record Metadata(long createdAt, long updatedAt, String note, String tags, String author, String server) {
        public Metadata(long createdAt, long updatedAt, String note, String tags) {
            this(createdAt, updatedAt, note, tags, "", "");
        }

        public Metadata(long createdAt, long updatedAt, String note, String tags, String author) {
            this(createdAt, updatedAt, note, tags, author, "");
        }
    }
}
