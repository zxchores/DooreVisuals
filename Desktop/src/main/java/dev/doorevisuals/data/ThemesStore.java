package dev.doorevisuals.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doorevisuals.draw.Theme;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ThemesStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final ThemesStore.MixedPreset[] MIXED = new ThemesStore.MixedPreset[]{
        new ThemesStore.MixedPreset("\u0421\u0438\u043d\u0438\u0439+\u041a\u0440\u0430\u0441\u043d\u044b\u0439", -12944385, -50614),
        new ThemesStore.MixedPreset("\u0424\u0438\u043e\u043b\u0435\u0442+\u0411\u0435\u043b\u044b\u0439", -5215489, -657926),
        new ThemesStore.MixedPreset("\u0413\u043e\u043b\u0443\u0431\u043e\u0439+\u0427\u0451\u0440\u043d\u044b\u0439", -10565377, -15592936),
        new ThemesStore.MixedPreset("\u0418\u0437\u0443\u043c\u0440\u0443\u0434+\u0417\u043e\u043b\u043e\u0442\u043e", -12533600, -14262),
        new ThemesStore.MixedPreset("\u0420\u043e\u0437\u0430+\u0423\u0433\u043e\u043b\u044c", -38232, -14013902),
        new ThemesStore.MixedPreset("\u041e\u0433\u043e\u043d\u044c+\u041b\u0451\u0434", -38358, -8727297)
    };

    private ThemesStore() {
    }

    public static List<String> list() {
        List<String> list = new ArrayList<>();
        Path path = ClientPaths.themes();
        if (!Files.isDirectory(path)) {
            return list;
        } else {
            try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(path, "*.json")) {
                for (Path path1 : directorystream) {
                    String s = path1.getFileName().toString();
                    list.add(s.substring(0, s.length() - 5));
                }
            } catch (Exception exception) {
            }

            return Favorites.sortThemes(list);
        }
    }

    public static boolean save(String name, int gui, int hud, int effect, int secondary) {
        String s = clean(name);
        if (s.isEmpty()) {
            return false;
        } else {
            try {
                JsonObject jsonobject = new JsonObject();
                jsonobject.addProperty("gui", gui);
                jsonobject.addProperty("hud", hud);
                jsonobject.addProperty("effect", effect);
                jsonobject.addProperty("secondary", secondary);
                Files.writeString(ClientPaths.themes().resolve(s + ".json"), GSON.toJson(jsonobject), StandardCharsets.UTF_8);
                return true;
            } catch (Exception exception) {
                return false;
            }
        }
    }

    public static JsonObject loadRaw(String name) {
        try {
            Path path = ClientPaths.themes().resolve(clean(name) + ".json");
            return !Files.exists(path) ? null : JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) {
            return null;
        }
    }

    public static boolean delete(String name) {
        try {
            boolean flag = Files.deleteIfExists(ClientPaths.themes().resolve(clean(name) + ".json"));
            if (flag) {
                Favorites.removeTheme(name);
            }

            return flag;
        } catch (Exception exception) {
            return false;
        }
    }

    public static boolean openFolder() {
        return Folders.open(ClientPaths.themes());
    }

    public static void applyJson(JsonObject o) {
        if (o != null) {
            int i = o.has("gui") ? o.get("gui").getAsInt() : Theme.ACCENT;
            int j = o.has("hud") ? o.get("hud").getAsInt() : i;
            int k = o.has("effect") ? o.get("effect").getAsInt() : i;
            int l = o.has("secondary") ? o.get("secondary").getAsInt() : Theme.ACCENT_HOT;
            Theme.applyMixed(i, l, j, k);
        }
    }

    public static void applyMixedPreset(ThemesStore.MixedPreset p) {
        Theme.applyMixed(p.primary(), p.secondary(), p.primary(), p.secondary());
    }

    public static String clean(String name) {
        if (name != null && !name.isBlank()) {
            String s = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");

            while (s.startsWith("-") || s.startsWith(".")) {
                s = s.substring(1);
            }

            return s.length() > 48 ? s.substring(0, 48) : s;
        } else {
            return "my-theme";
        }
    }

    public record MixedPreset(String label, int primary, int secondary) {
    }
}
