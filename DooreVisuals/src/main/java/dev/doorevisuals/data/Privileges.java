package dev.doorevisuals.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Privileges {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Privileges");
    private static final String REMOTE = "https://raw.githubusercontent.com/zxchores/DooreVisuals/main/privileges.json";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6L)).build();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "DoorePrivileges");
        thread.setDaemon(true);
        return thread;
    });
    private static final Pattern ROLE_ID = Pattern.compile("^[a-z][a-z0-9_]{0,15}$");
    private static final Map<String, String> ROLES = new ConcurrentHashMap<>();
    private static final Map<String, String> LABELS = new ConcurrentHashMap<>();

    private Privileges() {
    }

    public static void refresh() {
        loadFile(ClientPaths.privilegesFile());
        IO.execute(
            () -> {
                try {
                    HttpRequest httprequest = HttpRequest.newBuilder(URI.create("https://raw.githubusercontent.com/zxchores/DooreVisuals/main/privileges.json"))
                        .timeout(Duration.ofSeconds(8L))
                        .GET()
                        .build();
                    HttpResponse<String> httpresponse = HTTP.send(httprequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (httpresponse.statusCode() == 200) {
                        applyJson(httpresponse.body(), false);
                    }
                } catch (Exception exception) {
                    LOG.debug("privileges fetch: {}", exception.toString());
                }

                loadFile(ClientPaths.privilegesFile());
            }
        );
    }

    public static String roleOf(String uid) {
        return uid != null && !uid.isBlank() ? ROLES.getOrDefault(uid, "user") : "user";
    }

    public static String labelOf(String roleId) {
        String s = sanitize(roleId);
        if (!s.isBlank() && !"user".equals(s)) {
            String s1 = LABELS.get(s);
            return s1 != null && !s1.isBlank() ? s1 : s;
        } else {
            return "player";
        }
    }

    private static void loadFile(Path file) {
        if (Files.isRegularFile(file)) {
            try {
                applyJson(Files.readString(file), true);
            } catch (Exception exception) {
                LOG.debug("privileges file: {}", exception.toString());
            }
        }
    }

    private static void applyJson(String raw, boolean override) {
        JsonObject jsonobject = JsonParser.parseString(raw).getAsJsonObject();
        if (jsonobject.has("roles") && jsonobject.get("roles").isJsonArray()) {
            for (JsonElement jsonelement : jsonobject.getAsJsonArray("roles")) {
                if (jsonelement.isJsonObject()) {
                    JsonObject jsonobject1 = jsonelement.getAsJsonObject();
                    if (jsonobject1.has("id")) {
                        String s = sanitize(jsonobject1.get("id").getAsString());
                        if (!s.isBlank()) {
                            String s1 = jsonobject1.has("label") ? jsonobject1.get("label").getAsString() : s;
                            if (s1 == null || s1.isBlank()) {
                                s1 = s;
                            }

                            LABELS.put(s, s1);
                        }
                    }
                }
            }
        }

        if (jsonobject.has("users") && jsonobject.get("users").isJsonArray()) {
            for (JsonElement jsonelement1 : jsonobject.getAsJsonArray("users")) {
                if (jsonelement1.isJsonObject()) {
                    JsonObject jsonobject2 = jsonelement1.getAsJsonObject();
                    if (jsonobject2.has("uid") && jsonobject2.has("role")) {
                        String s3 = jsonobject2.get("uid").getAsString();
                        if (s3 != null && !s3.isBlank() && !"692344".equals(s3)) {
                            String s2 = sanitize(jsonobject2.get("role").getAsString());
                            if (!s3.isBlank() && !s2.isBlank() && (override || !ROLES.containsKey(s3))) {
                                ROLES.put(s3, s2);
                            }
                        }
                    }
                }
            }
        }
    }

    static String sanitize(String role) {
        String s = role == null ? "user" : role.trim().toLowerCase(Locale.ROOT);
        if (s.isBlank()) {
            return "user";
        } else {
            return ROLE_ID.matcher(s).matches() ? s : "user";
        }
    }
}
