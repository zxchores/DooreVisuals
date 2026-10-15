package dev.doorevisuals.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Compares two saved configs: which modules are on or off in one and not in the other, and which options differ. */
public final class ConfigDiff {
    private ConfigDiff() {
    }

    /** The differences, one per line; null if a file cannot be read. */
    public static List<String> compare(Path a, Path b) {
        JsonObject fa = features(a);
        JsonObject fb = features(b);
        if (fa == null || fb == null) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>(fa.keySet());
        ids.addAll(fb.keySet());
        for (String id : ids) {
            JsonObject ma = fa.has(id) && fa.get(id).isJsonObject() ? fa.getAsJsonObject(id) : new JsonObject();
            JsonObject mb = fb.has(id) && fb.get(id).isJsonObject() ? fb.getAsJsonObject(id) : new JsonObject();
            String onA = flag(ma);
            String onB = flag(mb);
            if (!onA.equals(onB)) {
                lines.add(id + ": " + onA + " → " + onB);
            }
            JsonObject oa = opts(ma);
            JsonObject ob = opts(mb);
            Set<String> keys = new LinkedHashSet<>(oa.keySet());
            keys.addAll(ob.keySet());
            for (String key : keys) {
                String va = oa.has(key) ? oa.get(key).toString() : "—";
                String vb = ob.has(key) ? ob.get(key).toString() : "—";
                if (!va.equals(vb)) {
                    lines.add(id + "." + key + ": " + trim(va) + " → " + trim(vb));
                }
            }
        }
        return lines;
    }

    private static String flag(JsonObject feature) {
        if (!feature.has("on")) {
            return "—";
        }
        return feature.get("on").getAsBoolean() ? "вкл" : "выкл";
    }

    private static JsonObject opts(JsonObject feature) {
        return feature.has("opts") && feature.get("opts").isJsonObject() ? feature.getAsJsonObject("opts") : new JsonObject();
    }

    private static String trim(String v) {
        return v.length() > 24 ? v.substring(0, 23) + "…" : v;
    }

    private static JsonObject features(Path file) {
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (root.isJsonObject() && root.getAsJsonObject().has("features") && root.getAsJsonObject().get("features").isJsonObject()) {
                return root.getAsJsonObject().getAsJsonObject("features");
            }
        } catch (Exception error) {
            // An unreadable or foreign file has nothing to compare.
        }
        return null;
    }
}
