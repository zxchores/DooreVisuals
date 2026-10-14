package dev.doorevisuals.world.sky;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doorevisuals.draw.Theme;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One Atmosphere preset as read from {@code assets/doorevisuals/atmosphere/presets.json}. Both the
 * option values the preset writes and the colours the sky is painted with live in the same record,
 * so a preset is described exactly once.
 */
public record SkyPreset(
    int day,
    int dawn,
    int dusk,
    int fogColor,
    float strength,
    String time,
    String weather,
    float fogDensity,
    float fogStart,
    float fogEnd,
    boolean aurora,
    boolean clouds,
    int zenith,
    int horizon,
    int nadir,
    int sun,
    int moon,
    int cloud,
    float cloudMix,
    int auroraA,
    int auroraB,
    float turbidity,
    float coverage
) {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Atmosphere");
    private static final String PATH = "/assets/doorevisuals/atmosphere/presets.json";
    private static final SkyPreset FALLBACK = new SkyPreset(
        -12533600,
        -29607,
        -4236839,
        -12533600,
        0.28F,
        "Ваниль",
        "Ваниль",
        0.9F,
        0.28F,
        0.92F,
        false,
        true,
        -11884328,
        -3610369,
        -15069152,
        -3896,
        -1510145,
        -722177,
        0.22F,
        -12533600,
        -4236839,
        0.42F,
        0.5F
    );
    private static Map<String, SkyPreset> table;

    public static SkyPreset of(String name) {
        SkyPreset skypreset = table().get(name);
        return skypreset != null ? skypreset : FALLBACK;
    }

    public static boolean has(String name) {
        return table().containsKey(name);
    }

    /** Mixes the preset cloud colour toward the accent the way the old hardcoded table did. */
    public int cloudTint(int accent) {
        return Theme.tintMul(this.cloud, accent, this.cloudMix);
    }

    private static Map<String, SkyPreset> table() {
        Map<String, SkyPreset> map = table;
        if (map == null) {
            map = load();
            table = map;
        }

        return map;
    }

    private static Map<String, SkyPreset> load() {
        try (InputStream inputstream = SkyPreset.class.getResourceAsStream(PATH)) {
            if (inputstream == null) {
                LOG.error("Atmosphere presets missing at {}", PATH);
                return Map.of();
            }

            JsonObject jsonobject = JsonParser.parseReader(new InputStreamReader(inputstream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, SkyPreset> map = new LinkedHashMap<>();

            for (Map.Entry<String, JsonElement> entry : jsonobject.entrySet()) {
                map.put(entry.getKey(), parse(entry.getValue().getAsJsonObject()));
            }

            return Collections.unmodifiableMap(map);
        } catch (Exception exception) {
            LOG.error("Atmosphere presets failed to load", exception);
            return Map.of();
        }
    }

    private static SkyPreset parse(JsonObject o) {
        JsonObject jsonobject = o.getAsJsonObject("sky");
        return new SkyPreset(
            tint(o, "day", FALLBACK.day),
            tint(o, "dawn", FALLBACK.dawn),
            tint(o, "dusk", FALLBACK.dusk),
            tint(o, "fogColor", FALLBACK.fogColor),
            num(o, "strength", FALLBACK.strength),
            str(o, "time", FALLBACK.time),
            str(o, "weather", FALLBACK.weather),
            num(o, "fogDensity", FALLBACK.fogDensity),
            num(o, "fogStart", FALLBACK.fogStart),
            num(o, "fogEnd", FALLBACK.fogEnd),
            flag(o, "aurora", FALLBACK.aurora),
            flag(o, "clouds", FALLBACK.clouds),
            tint(jsonobject, "zenith", FALLBACK.zenith),
            tint(jsonobject, "horizon", FALLBACK.horizon),
            tint(jsonobject, "nadir", FALLBACK.nadir),
            tint(jsonobject, "sun", FALLBACK.sun),
            tint(jsonobject, "moon", FALLBACK.moon),
            tint(jsonobject, "cloud", FALLBACK.cloud),
            num(jsonobject, "cloudMix", FALLBACK.cloudMix),
            tint(jsonobject, "auroraA", FALLBACK.auroraA),
            tint(jsonobject, "auroraB", FALLBACK.auroraB),
            num(jsonobject, "turbidity", FALLBACK.turbidity),
            num(jsonobject, "coverage", FALLBACK.coverage)
        );
    }

    private static int tint(JsonObject o, String key, int fallback) {
        if (o == null || !o.has(key)) {
            return fallback;
        } else {
            String s = o.get(key).getAsString().trim();
            if (s.startsWith("#")) {
                s = s.substring(1);
            }

            try {
                long i = Long.parseLong(s, 16);
                return s.length() <= 6 ? (int)(i | 0xFF000000L) : (int)i;
            } catch (NumberFormatException numberformatexception) {
                LOG.warn("Atmosphere preset colour {} is not hex: {}", key, s);
                return fallback;
            }
        }
    }

    private static float num(JsonObject o, String key, float fallback) {
        return o != null && o.has(key) ? o.get(key).getAsFloat() : fallback;
    }

    private static boolean flag(JsonObject o, String key, boolean fallback) {
        return o != null && o.has(key) ? o.get(key).getAsBoolean() : fallback;
    }

    private static String str(JsonObject o, String key, String fallback) {
        return o != null && o.has(key) ? o.get(key).getAsString() : fallback;
    }
}
