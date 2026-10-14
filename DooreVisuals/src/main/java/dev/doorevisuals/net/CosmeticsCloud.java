package dev.doorevisuals.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doorevisuals.cosmetics.CosmeticsRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CosmeticsCloud {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Cloud");
    private static final String FALLBACK_REGISTRY_URL = "https://raw.githubusercontent.com/zxchores/DooreVisuals/main/tools/cosmetics-registry.json";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6L)).build();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "DooreCloud");
        thread.setDaemon(true);
        return thread;
    });

    private CosmeticsCloud() {
    }

    private static String endpoint() {
        return System.getProperty("doorevisuals.cosmeticsApi", "https://raw.githubusercontent.com/zxchores/DooreVisuals/main/tools/cosmetics-registry.json");
    }

    public static void fetch(UUID self) {
        if (self != null) {
            IO.execute(() -> {
                try {
                    String s = endpoint();
                    String s1 = s.contains("/v1/") ? s + "/" + self : s;
                    HttpRequest httprequest = HttpRequest.newBuilder(URI.create(s1)).timeout(Duration.ofSeconds(8L)).GET().build();
                    HttpResponse<String> httpresponse = HTTP.send(httprequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (httpresponse.statusCode() != 200) {
                        return;
                    }

                    JsonObject jsonobject = JsonParser.parseString(httpresponse.body()).getAsJsonObject();
                    if (jsonobject.has("uuid")) {
                        String s5 = jsonobject.has("cape") ? jsonobject.get("cape").getAsString() : "";
                        String s6 = jsonobject.has("wings") ? jsonobject.get("wings").getAsString() : "";
                        CosmeticsRegistry.setLook(self, s5, s6);
                        return;
                    }

                    if (!jsonobject.has("entries") || !jsonobject.get("entries").isJsonObject()) {
                        return;
                    }

                    JsonObject jsonobject1 = jsonobject.getAsJsonObject("entries");

                    for (String s2 : jsonobject1.keySet()) {
                        try {
                            UUID uuid = UUID.fromString(s2);
                            JsonObject jsonobject2 = jsonobject1.getAsJsonObject(s2);
                            String s3 = jsonobject2.has("cape") ? jsonobject2.get("cape").getAsString() : "";
                            String s4 = jsonobject2.has("wings") ? jsonobject2.get("wings").getAsString() : "";
                            CosmeticsRegistry.setLook(uuid, s3, s4);
                        } catch (Exception exception) {
                        }
                    }

                    LOG.info("Cloud cosmetics registry loaded");
                } catch (Exception exception1) {
                    LOG.debug("Cloud fetch failed: {}", exception1.toString());
                }
            });
        }
    }

    public static void push(UUID self, String capeId, String wingsId) {
        if (self != null) {
            IO.execute(
                () -> {
                    String s = endpoint();
                    if (!s.contains("/v1/")) {
                        LOG.info("Cloud registry is read-only; peer sync remains active");
                    } else {
                        try {
                            JsonObject jsonobject = new JsonObject();
                            jsonobject.addProperty("cape", capeId == null ? "" : capeId);
                            jsonobject.addProperty("wings", wingsId == null ? "" : wingsId);
                            HttpRequest httprequest = HttpRequest.newBuilder(URI.create(s + "/" + self))
                                .timeout(Duration.ofSeconds(8L))
                                .header("Content-Type", "application/json")
                                .header("X-Doore-Token", System.getProperty("doorevisuals.cloudToken", "dev-session"))
                                .PUT(BodyPublishers.ofString(jsonobject.toString(), StandardCharsets.UTF_8))
                                .build();
                            HttpResponse<Void> httpresponse = HTTP.send(httprequest, BodyHandlers.discarding());
                            LOG.info("Cloud cosmetics push {} \u2192 HTTP {}", self, httpresponse.statusCode());
                        } catch (Exception exception) {
                            LOG.debug("Cloud push failed: {}", exception.toString());
                        }
                    }
                }
            );
        }
    }
}
