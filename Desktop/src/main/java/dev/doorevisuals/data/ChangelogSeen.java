package dev.doorevisuals.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChangelogSeen {
    private ChangelogSeen() {
    }

    public static boolean hasNew() {
        return !"3.30.2".equals(lastSeen());
    }

    public static String lastSeen() {
        try {
            Path path = ClientPaths.state();
            if (!Files.exists(path)) {
                return "";
            } else {
                JsonObject jsonobject = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
                return jsonobject.has("last_changelog") ? jsonobject.get("last_changelog").getAsString() : "";
            }
        } catch (Exception exception) {
            return "";
        }
    }

    public static void markSeen() {
        try {
            Path path = ClientPaths.state();
            Files.createDirectories(ClientPaths.root());
            JsonObject jsonobject = new JsonObject();
            if (Files.exists(path)) {
                jsonobject = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            }

            jsonobject.addProperty("last_changelog", "3.30.2");
            Files.writeString(path, jsonobject.toString(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
        }
    }
}
