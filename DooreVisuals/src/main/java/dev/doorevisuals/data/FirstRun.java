package dev.doorevisuals.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FirstRun {
    private FirstRun() {
    }

    public static boolean needsWizard() {
        try {
            Path path = ClientPaths.state();
            if (!Files.exists(path)) {
                return true;
            } else {
                JsonObject jsonobject = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
                return !jsonobject.has("first_run_done") || !jsonobject.get("first_run_done").getAsBoolean();
            }
        } catch (Exception exception) {
            return true;
        }
    }

    public static void markDone() {
        try {
            Path path = ClientPaths.state();
            Files.createDirectories(ClientPaths.root());
            JsonObject jsonobject = new JsonObject();
            if (Files.exists(path)) {
                jsonobject = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            }

            jsonobject.addProperty("first_run_done", true);
            Files.writeString(path, jsonobject.toString(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
        }
    }
}
