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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Favorites {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> THEMES = new LinkedHashSet<>();
    private static final Set<String> CONFIGS = new LinkedHashSet<>();
    private static final Set<String> MODULES = new LinkedHashSet<>();
    private static boolean loaded;

    private Favorites() {
    }

    public static synchronized void ensure() {
        if (!loaded) {
            loaded = true;
            Path path = ClientPaths.root().resolve("favorites.json");
            if (Files.exists(path)) {
                try {
                    JsonObject jsonobject = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
                    readList(jsonobject, "themes", THEMES);
                    readList(jsonobject, "configs", CONFIGS);
                    readList(jsonobject, "modules", MODULES);
                } catch (Exception exception) {
                }
            }
        }
    }

    private static void readList(JsonObject o, String key, Set<String> into) {
        into.clear();
        if (o.has(key) && o.get(key).isJsonArray()) {
            for (JsonElement jsonelement : o.getAsJsonArray(key)) {
                String s = clean(jsonelement.getAsString());
                if (!s.isEmpty()) {
                    into.add(s);
                }
            }
        }
    }

    private static void write() {
        try {
            JsonObject jsonobject = new JsonObject();
            jsonobject.add("themes", toArr(THEMES));
            jsonobject.add("configs", toArr(CONFIGS));
            jsonobject.add("modules", toArr(MODULES));
            Files.writeString(ClientPaths.root().resolve("favorites.json"), GSON.toJson(jsonobject), StandardCharsets.UTF_8);
        } catch (Exception exception) {
        }
    }

    private static JsonArray toArr(Set<String> set) {
        JsonArray jsonarray = new JsonArray();

        for (String s : set) {
            jsonarray.add(s);
        }

        return jsonarray;
    }

    public static synchronized boolean isThemeFav(String name) {
        ensure();
        return THEMES.contains(clean(name));
    }

    public static synchronized boolean isConfigFav(String name) {
        ensure();
        return CONFIGS.contains(clean(name));
    }

    public static synchronized void toggleTheme(String name) {
        ensure();
        String s = clean(name);
        if (!s.isEmpty()) {
            if (!THEMES.add(s)) {
                THEMES.remove(s);
            }

            write();
        }
    }

    public static synchronized void toggleConfig(String name) {
        ensure();
        String s = clean(name);
        if (!s.isEmpty()) {
            if (!CONFIGS.add(s)) {
                CONFIGS.remove(s);
            }

            write();
        }
    }

    public static synchronized boolean isModuleFav(String id) {
        ensure();
        return MODULES.contains(clean(id));
    }

    public static synchronized void toggleModule(String id) {
        ensure();
        String s = clean(id);
        if (!s.isEmpty()) {
            if (!MODULES.add(s)) {
                MODULES.remove(s);
            }

            write();
        }
    }

    public static synchronized void removeTheme(String name) {
        ensure();
        THEMES.remove(clean(name));
        write();
    }

    public static synchronized void removeConfig(String name) {
        ensure();
        CONFIGS.remove(clean(name));
        write();
    }

    public static synchronized List<String> sortThemes(List<String> names) {
        ensure();
        return sort(names, THEMES);
    }

    public static synchronized List<String> sortConfigs(List<String> names) {
        ensure();
        return sort(names, CONFIGS);
    }

    private static List<String> sort(List<String> names, Set<String> favs) {
        List<String> list = new ArrayList<>();
        List<String> list1 = new ArrayList<>();

        for (String s : names) {
            if (favs.contains(clean(s))) {
                list.add(s);
            } else {
                list1.add(s);
            }
        }

        list.sort(String.CASE_INSENSITIVE_ORDER);
        list1.sort(String.CASE_INSENSITIVE_ORDER);
        list.addAll(list1);
        return list;
    }

    private static String clean(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
    }
}
