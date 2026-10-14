package dev.doorevisuals.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Uuids;

public final class AltsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, AltsStore.Alt> ALTS = new LinkedHashMap<>();
    private static String active = "";
    private static boolean loaded;

    private AltsStore() {
    }

    public static synchronized void boot() {
        if (!loaded) {
            loaded = true;
            load();
            ensureCurrent();
        }
    }

    public static synchronized List<AltsStore.Alt> all() {
        boot();
        return new ArrayList<>(ALTS.values());
    }

    public static synchronized String activeName() {
        boot();
        if (!active.isBlank() && ALTS.containsKey(key(active))) {
            return ALTS.get(key(active)).name();
        } else {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient != null && minecraftclient.getSession() != null) {
                return minecraftclient.getSession().getUsername();
            } else {
                return active.isBlank() ? "Player" : active;
            }
        }
    }

    public static synchronized boolean add(String raw) {
        boot();
        String s = clean(raw);
        if (s.isEmpty()) {
            return false;
        } else {
            String s1 = key(s);
            if (ALTS.containsKey(s1)) {
                return false;
            } else {
                ALTS.put(s1, new AltsStore.Alt(s, Uuids.getOfflinePlayerUuid(s)));
                save();
                return true;
            }
        }
    }

    public static synchronized boolean remove(String raw) {
        boot();
        String s = key(clean(raw));
        if (!s.isEmpty() && ALTS.containsKey(s)) {
            ALTS.remove(s);
            if (key(active).equals(s)) {
                active = ALTS.isEmpty() ? "" : ALTS.values().iterator().next().name();
            }

            save();
            return true;
        } else {
            return false;
        }
    }

    public static synchronized void markActive(String raw) {
        boot();
        String s = clean(raw);
        if (!s.isEmpty()) {
            String s1 = key(s);
            if (!ALTS.containsKey(s1)) {
                ALTS.put(s1, new AltsStore.Alt(s, Uuids.getOfflinePlayerUuid(s)));
            }

            active = ALTS.get(s1).name();
            save();
        }
    }

    public static synchronized AltsStore.Alt find(String raw) {
        boot();
        return ALTS.get(key(clean(raw)));
    }

    private static void ensureCurrent() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient != null && minecraftclient.getSession() != null) {
            String s = clean(minecraftclient.getSession().getUsername());
            if (!s.isEmpty()) {
                String s1 = key(s);
                if (!ALTS.containsKey(s1)) {
                    ALTS.put(s1, new AltsStore.Alt(s, minecraftclient.getSession().getUuidOrNull()));
                }

                if (active.isBlank()) {
                    active = s;
                }

                save();
            }
        }
    }

    private static Path file() {
        return ClientPaths.root().resolve("alts.json");
    }

    private static void load() {
        Path path = file();
        if (Files.isRegularFile(path)) {
            try {
                JsonObject jsonobject = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
                if (jsonobject.has("active")) {
                    active = clean(jsonobject.get("active").getAsString());
                }

                if (jsonobject.has("alts") && jsonobject.get("alts").isJsonArray()) {
                    for (JsonElement jsonelement : jsonobject.getAsJsonArray("alts")) {
                        if (jsonelement.isJsonObject()) {
                            JsonObject jsonobject1 = jsonelement.getAsJsonObject();
                            String s = clean(jsonobject1.has("name") ? jsonobject1.get("name").getAsString() : "");
                            if (!s.isEmpty()) {
                                UUID uuid;
                                try {
                                    uuid = jsonobject1.has("uuid") ? UUID.fromString(jsonobject1.get("uuid").getAsString()) : Uuids.getOfflinePlayerUuid(s);
                                } catch (Exception exception) {
                                    uuid = Uuids.getOfflinePlayerUuid(s);
                                }

                                ALTS.put(key(s), new AltsStore.Alt(s, uuid));
                            }
                        }
                    }
                }
            } catch (Exception exception1) {
            }
        }
    }

    private static void save() {
        try {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("active", active);
            JsonArray jsonarray = new JsonArray();

            for (AltsStore.Alt altsstore$alt : ALTS.values()) {
                JsonObject jsonobject1 = new JsonObject();
                jsonobject1.addProperty("name", altsstore$alt.name());
                jsonobject1.addProperty("uuid", altsstore$alt.uuid().toString());
                jsonarray.add(jsonobject1);
            }

            jsonobject.add("alts", jsonarray);
            Files.writeString(file(), GSON.toJson(jsonobject), StandardCharsets.UTF_8);
        } catch (Exception exception) {
        }
    }

    private static String clean(String s) {
        if (s == null) {
            return "";
        } else {
            String sx = s.trim();
            if (sx.length() > 16) {
                sx = sx.substring(0, 16);
            }

            return sx.replaceAll("[^A-Za-z0-9_]", "");
        }
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public record Alt(String name, UUID uuid) {
    }
}
