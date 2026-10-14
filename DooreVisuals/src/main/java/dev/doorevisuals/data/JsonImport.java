package dev.doorevisuals.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doorevisuals.App;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.tools.ConfigFeature;
import dev.doorevisuals.tools.ThemeFeature;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class JsonImport {
    private JsonImport() {
    }

    public static JsonImport.Kind detect(Path file) {
        if (file != null && Files.isRegularFile(file)) {
            String s = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (s.endsWith(".cfg")) {
                return isConfigJson(file) ? JsonImport.Kind.CONFIG : JsonImport.Kind.UNKNOWN;
            } else if (!s.endsWith(".json")) {
                return JsonImport.Kind.UNKNOWN;
            } else {
                try {
                    JsonObject jsonobject = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                    if ((jsonobject.has("gui") || jsonobject.has("hud") || jsonobject.has("effect") || jsonobject.has("secondary"))
                        && !jsonobject.has("features")
                        && !jsonobject.has("layout")
                        && !jsonobject.has("v")) {
                        return JsonImport.Kind.THEME;
                    }

                    if (jsonobject.has("gui") && jsonobject.has("effect")) {
                        return JsonImport.Kind.THEME;
                    }
                } catch (Exception exception) {
                }

                return JsonImport.Kind.UNKNOWN;
            }
        } else {
            return JsonImport.Kind.UNKNOWN;
        }
    }

    private static boolean isConfigJson(Path file) {
        try {
            JsonObject jsonobject = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            return jsonobject.has("features") || jsonobject.has("layout") || jsonobject.has("v") || jsonobject.has("_meta");
        } catch (Exception exception) {
            return false;
        }
    }

    public static boolean importFile(Path file, JsonImport.Kind prefer) {
        JsonImport.Kind jsonimport$kind = prefer != JsonImport.Kind.UNKNOWN ? prefer : detect(file);
        if (jsonimport$kind == JsonImport.Kind.UNKNOWN) {
            jsonimport$kind = detect(file);
        }
        return switch (jsonimport$kind) {
            case THEME -> importTheme(file);
            case CONFIG -> importConfig(file);
            default -> false;
        };
    }

    public static boolean importTheme(Path file) {
        try {
            String s = stripExt(file.getFileName().toString());
            String s1 = ThemesStore.clean(s);
            Path path = ClientPaths.themes().resolve(s1 + ".json");
            Files.createDirectories(ClientPaths.themes());
            Files.copy(file, path, StandardCopyOption.REPLACE_EXISTING);
            JsonObject jsonobject = ThemesStore.loadRaw(s1);
            ThemesStore.applyJson(jsonobject);
            App.features().find(ThemeFeature.class).ifPresent(tf -> tf.setDraftName(s1));
            toast("\u0422\u0435\u043c\u0430", "\u0418\u043c\u043f\u043e\u0440\u0442: " + s1);
            return true;
        } catch (Exception exception) {
            toast("\u0422\u0435\u043c\u0430", "\u041e\u0448\u0438\u0431\u043a\u0430 \u0438\u043c\u043f\u043e\u0440\u0442\u0430");
            return false;
        }
    }

    public static boolean importConfig(Path file) {
        try {
            if (file != null && Files.isRegularFile(file)) {
                String s = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!s.endsWith(".cfg")) {
                    toast("\u041a\u043e\u043d\u0444\u0438\u0433", "\u0422\u043e\u043b\u044c\u043a\u043e .cfg");
                    return false;
                } else {
                    String s1 = stripExt(file.getFileName().toString());
                    String s2 = Profiles.clean(s1);
                    if (s2.isEmpty()) {
                        s2 = "imported";
                    }

                    String s3 = s2;
                    Files.createDirectories(ClientPaths.configs());
                    Path path = ClientPaths.configs().resolve(s3 + ".cfg");
                    Files.copy(file, path, StandardCopyOption.REPLACE_EXISTING);
                    boolean flag = App.live() && App.save().profiles().load(s3);
                    if (flag) {
                        App.features().find(ConfigFeature.class).ifPresent(c -> c.syncName(s3));
                        toast("\u041a\u043e\u043d\u0444\u0438\u0433", "Config downloaded");
                    } else {
                        String s4 = App.live() ? App.save().profiles().lastError() : "";
                        toast(
                            "\u041a\u043e\u043d\u0444\u0438\u0433",
                            s4.isEmpty() ? "\u0424\u0430\u0439\u043b \u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d: " + s3 + ".cfg" : s4
                        );
                    }

                    return true;
                }
            } else {
                return false;
            }
        } catch (Exception exception) {
            toast("\u041a\u043e\u043d\u0444\u0438\u0433", "\u041e\u0448\u0438\u0431\u043a\u0430 \u0438\u043c\u043f\u043e\u0440\u0442\u0430");
            return false;
        }
    }

    private static String stripExt(String name) {
        String s = name.toLowerCase(Locale.ROOT);
        if (s.endsWith(".cfg")) {
            return name.substring(0, name.length() - 4);
        } else {
            return s.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
        }
    }

    private static void toast(String title, String body) {
        App.features().find(HudFeature.class).ifPresent(h -> h.notify(title, body));
    }

    public static enum Kind {
        THEME,
        CONFIG,
        UNKNOWN;
    }
}
