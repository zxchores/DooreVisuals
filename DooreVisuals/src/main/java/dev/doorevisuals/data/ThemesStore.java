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
        new ThemesStore.MixedPreset("\u041e\u0433\u043e\u043d\u044c+\u041b\u0451\u0434", -38358, -8727297),
        new ThemesStore.MixedPreset("Роза+Лаванда", 0xFFE0899F, 0xFFB79CFF),
        new ThemesStore.MixedPreset("Мята+Графит", 0xFF5EEAD4, 0xFF1F2937),
        new ThemesStore.MixedPreset("Закат", 0xFFFB923C, 0xFFEC4899),
        new ThemesStore.MixedPreset("Океан", 0xFF38BDF8, 0xFF1E3A8A),
        new ThemesStore.MixedPreset("Сакура", 0xFFF9A8D4, 0xFFFFFFFF),
        new ThemesStore.MixedPreset("Золото+Ночь", 0xFFFACC15, 0xFF1C1917),
        new ThemesStore.MixedPreset("Лайм+Лес", 0xFFA3E635, 0xFF14532D),
        new ThemesStore.MixedPreset("Рубин", 0xFFEF4444, 0xFF7F1D1D),
        new ThemesStore.MixedPreset("Аметист", 0xFFA855F7, 0xFF4C1D95),
        new ThemesStore.MixedPreset("Бирюза+Коралл", 0xFF14B8A6, 0xFFFB7185),
        new ThemesStore.MixedPreset("Индиго", 0xFF6366F1, 0xFFC7D2FE),
        new ThemesStore.MixedPreset("Графит", 0xFF9CA3AF, 0xFF111827),
        new ThemesStore.MixedPreset("Персик", 0xFFFDBA74, 0xFFFDA4AF),
        new ThemesStore.MixedPreset("Неон", 0xFF22D3EE, 0xFFE879F9)
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
