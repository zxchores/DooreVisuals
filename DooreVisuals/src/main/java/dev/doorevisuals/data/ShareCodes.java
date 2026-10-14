package dev.doorevisuals.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doorevisuals.App;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Skin;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.tools.ConfigFeature;
import dev.doorevisuals.tools.ThemeFeature;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ShareCodes {
    public static final String THEME_V1 = "DV-T1.";
    public static final String THEME_V2 = "DV-T2.";
    public static final String CONFIG_V1 = "DV-C1.";
    public static final String CONFIG_V2 = "DV-C2.";
    public static final int LONG_WARN = 1500;

    private ShareCodes() {
    }

    public static String encodeActiveTheme() {
        ThemeFeature themefeature = App.live() ? App.features().find(ThemeFeature.class).orElse(null) : null;
        int i = Theme.ACCENT;
        int j = Theme.HUD;
        int k = Theme.EFFECT;
        int l = Theme.ACCENT_HOT;
        String s = "\u0421\u0432\u043e\u044f";
        if (themefeature != null) {
            i = themefeature.channelColor(0);
            j = themefeature.channelColor(1);
            k = themefeature.channelColor(2);
            l = themefeature.channelColor(3);
            s = themefeature.presetLabel();
        }

        return encodeThemeV2(i, j, k, l, s);
    }

    public static String encodeThemeV2(int gui, int hud, int effect, int secondary, String preset) {
        int i = presetIndex(preset);
        String s = hex(gui) + hex(hud) + hex(effect) + hex(secondary) + String.format(Locale.ROOT, "%02X", i);
        return "DV-T2." + s + crcNibble(s);
    }

    public static String encodeActiveConfig() {
        return !App.live() ? "" : encodeConfigV2(compactSnapshot(App.save().snapshot()));
    }

    public static String encodeConfigV2(JsonObject compact) {
        try {
            byte[] abyte = compact.toString().getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();

            try (GZIPOutputStream gzipoutputstream = new GZIPOutputStream(bytearrayoutputstream)) {
                gzipoutputstream.write(abyte);
            }

            String s = Base64.getUrlEncoder().withoutPadding().encodeToString(bytearrayoutputstream.toByteArray());
            return "DV-C2." + s + "." + crcNibble(s);
        } catch (Exception exception) {
            return "";
        }
    }

    public static String encodeConfigFull(JsonObject root) {
        try {
            byte[] abyte = root.toString().getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();

            try (GZIPOutputStream gzipoutputstream = new GZIPOutputStream(bytearrayoutputstream)) {
                gzipoutputstream.write(abyte);
            }

            return "DV-C1." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytearrayoutputstream.toByteArray());
        } catch (Exception exception) {
            return "";
        }
    }

    public static boolean isLong(String code) {
        return code != null && code.length() > 1500;
    }

    public static String tryApply(String raw) {
        if (raw == null) {
            return null;
        } else {
            String s = raw.trim().replaceAll("\\s+", "");
            if (s.regionMatches(true, 0, "DV-T2.", 0, "DV-T2.".length())) {
                return applyThemeV2(s.substring("DV-T2.".length()))
                    ? "\u0442\u0435\u043c\u0430 \u043f\u0440\u0438\u043c\u0435\u043d\u0435\u043d\u0430"
                    : "\u0431\u0438\u0442\u044b\u0439 \u043a\u043e\u0434 \u0442\u0435\u043c\u044b";
            } else if (s.regionMatches(true, 0, "DV-T1.", 0, "DV-T1.".length())) {
                return applyThemeV1(s.substring("DV-T1.".length()))
                    ? "\u0442\u0435\u043c\u0430 \u043f\u0440\u0438\u043c\u0435\u043d\u0435\u043d\u0430"
                    : "\u0431\u0438\u0442\u044b\u0439 \u043a\u043e\u0434 \u0442\u0435\u043c\u044b";
            } else if (s.regionMatches(true, 0, "DV-C2.", 0, "DV-C2.".length())) {
                boolean flag1 = applyConfigV2(s.substring("DV-C2.".length()));
                if (flag1) {
                    syncConfigName();
                    return "\u043a\u043e\u043d\u0444\u0438\u0433 \u00b7 " + ConfigFeature.activeName();
                } else {
                    return "\u0431\u0438\u0442\u044b\u0439 \u043a\u043e\u0434 \u043a\u043e\u043d\u0444\u0438\u0433\u0430";
                }
            } else if (s.regionMatches(true, 0, "DV-C1.", 0, "DV-C1.".length())) {
                boolean flag = applyConfigV1(s.substring("DV-C1.".length()));
                if (flag) {
                    syncConfigName();
                    return "\u043a\u043e\u043d\u0444\u0438\u0433 \u00b7 " + ConfigFeature.activeName();
                } else {
                    return "\u0431\u0438\u0442\u044b\u0439 \u043a\u043e\u0434 \u043a\u043e\u043d\u0444\u0438\u0433\u0430";
                }
            } else {
                return null;
            }
        }
    }

    public static boolean looksLike(String raw) {
        if (raw == null) {
            return false;
        } else {
            String s = raw.trim();
            return starts(s, "DV-T2.") || starts(s, "DV-T1.") || starts(s, "DV-C2.") || starts(s, "DV-C1.");
        }
    }

    public static JsonObject compactSnapshot(JsonObject full) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("v", 5);
        jsonobject.addProperty("compact", true);
        if (full.has("layout")) {
            jsonobject.add("layout", full.get("layout"));
        }

        if (full.has("gps")) {
            jsonobject.add("gps", full.get("gps"));
        }

        JsonObject jsonobject1 = full.has("features") ? full.getAsJsonObject("features") : new JsonObject();
        JsonObject jsonobject2 = new JsonObject();
        if (App.live()) {
            for (Feature feature : App.features().all()) {
                JsonObject jsonobject3 = jsonobject1.getAsJsonObject(feature.id());
                boolean flag = jsonobject3 != null && jsonobject3.has("on") ? jsonobject3.get("on").getAsBoolean() : feature.on();
                JsonObject jsonobject4 = new JsonObject();
                if (jsonobject3 != null && jsonobject3.has("opts")) {
                    JsonObject jsonobject5 = jsonobject3.getAsJsonObject("opts");

                    for (Opt<?> opt : feature.opts()) {
                        JsonElement jsonelement = jsonobject5.get(opt.id());
                        if (jsonelement != null && (flag || !opt.isDefault()) && !opt.isDefault()) {
                            jsonobject4.add(opt.id(), jsonelement);
                        }
                    }
                }

                if (flag || jsonobject4.size() > 0) {
                    JsonObject jsonobject6 = new JsonObject();
                    jsonobject6.addProperty("on", flag);
                    if (jsonobject4.size() > 0) {
                        jsonobject6.add("opts", jsonobject4);
                    }

                    jsonobject2.add(feature.id(), jsonobject6);
                }
            }
        } else {
            jsonobject2 = jsonobject1;
        }

        jsonobject.add("features", jsonobject2);
        return jsonobject;
    }

    private static void syncConfigName() {
        if (App.live()) {
            String s = App.save().profiles().active();
            App.features().find(ConfigFeature.class).ifPresent(c -> c.syncName(s));
            App.features()
                .find(HudFeature.class)
                .ifPresent(h -> h.notify("\u041a\u043e\u043d\u0444\u0438\u0433", "\u043a\u043e\u0434 \u2192 " + s, HudFeature.NoteKind.OK));
        }
    }

    private static boolean applyThemeV2(String body) {
        if (body.length() < 35) {
            return false;
        } else {
            String s = body.substring(0, body.length() - 2);
            String s1 = body.substring(body.length() - 2);
            if (!s1.equalsIgnoreCase(crcNibble(s))) {
                return false;
            } else {
                try {
                    int i = parseHex(s.substring(0, 8));
                    int j = parseHex(s.substring(8, 16));
                    int k = parseHex(s.substring(16, 24));
                    int l = parseHex(s.substring(24, 32));
                    int i1 = Integer.parseInt(s.substring(32, 34), 16);
                    applyThemeColors(i, j, k, l, presetFromIndex(i1));
                    return true;
                } catch (Exception exception) {
                    return false;
                }
            }
        }
    }

    private static boolean applyThemeV1(String body) {
        if (body.length() < 32) {
            return false;
        } else {
            try {
                applyThemeColors(
                    parseHex(body.substring(0, 8)),
                    parseHex(body.substring(8, 16)),
                    parseHex(body.substring(16, 24)),
                    parseHex(body.substring(24, 32)),
                    "\u0421\u0432\u043e\u044f"
                );
                return true;
            } catch (Exception exception) {
                return false;
            }
        }
    }

    private static void applyThemeColors(int gui, int hud, int effect, int secondary, String preset) {
        ThemeFeature themefeature = App.features().find(ThemeFeature.class).orElse(null);
        if (themefeature != null) {
            if (preset != null && !"\u0421\u0432\u043e\u044f".equals(preset) && Skin.byLabel(preset) != Skin.CUSTOM) {
                themefeature.applyPreset(preset);
            }

            themefeature.setChannelColor(0, gui);
            themefeature.setChannelColor(1, hud);
            themefeature.setChannelColor(2, effect);
            themefeature.setChannelColor(3, secondary);
        } else {
            Theme.applyMixed(gui, secondary, hud, effect);
        }
    }

    private static boolean applyConfigV2(String body) {
        int i = body.lastIndexOf(46);
        if (i > 0 && i < body.length() - 1) {
            String s = body.substring(0, i);
            String s1 = body.substring(i + 1);
            return !s1.equalsIgnoreCase(crcNibble(s)) ? false : decodeAndApply(s);
        } else {
            return false;
        }
    }

    private static boolean applyConfigV1(String body) {
        return decodeAndApply(body);
    }

    private static boolean decodeAndApply(String b64) {
        if (App.live() && !b64.isEmpty()) {
            try {
                byte[] abyte = Base64.getUrlDecoder().decode(b64);
                ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();

                try (GZIPInputStream gzipinputstream = new GZIPInputStream(new ByteArrayInputStream(abyte))) {
                    gzipinputstream.transferTo(bytearrayoutputstream);
                }

                JsonObject jsonobject = JsonParser.parseString(bytearrayoutputstream.toString(StandardCharsets.UTF_8)).getAsJsonObject();
                App.save().apply(jsonobject);
                return true;
            } catch (Exception exception) {
                return false;
            }
        } else {
            return false;
        }
    }

    private static int presetIndex(String label) {
        Skin[] askin = Skin.values();

        for (int i = 0; i < askin.length; i++) {
            if (askin[i].label().equals(label)) {
                return i;
            }
        }

        return Skin.CUSTOM.ordinal();
    }

    private static String presetFromIndex(int id) {
        Skin[] askin = Skin.values();
        return id >= 0 && id < askin.length ? askin[id].label() : "\u0421\u0432\u043e\u044f";
    }

    private static String crcNibble(String s) {
        CRC32 crc32 = new CRC32();
        crc32.update(s.getBytes(StandardCharsets.UTF_8));
        return String.format(Locale.ROOT, "%02X", (int)(crc32.getValue() & 255L));
    }

    private static boolean starts(String s, String p) {
        return s.regionMatches(true, 0, p, 0, p.length());
    }

    private static String hex(int argb) {
        return String.format(Locale.ROOT, "%08X", argb | 0xFF000000);
    }

    private static int parseHex(String h) {
        return (int)Long.parseUnsignedLong(h, 16);
    }
}
