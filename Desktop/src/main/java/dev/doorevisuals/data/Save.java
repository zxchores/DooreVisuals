package dev.doorevisuals.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.FeatureBus;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.tools.GpsFeature;
import dev.doorevisuals.tools.GuiWindowsFeature;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Save {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Save");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final FeatureBus bus;
    private final Profiles profiles;
    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "DooreSave");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean applying;
    private final AtomicReference<ScheduledFuture<?>> pending = new AtomicReference<>();

    public Save(FeatureBus bus) {
        this.bus = bus;
        this.profiles = new Profiles(this::snapshot, this::apply);
        bus.onDirty(this::defer);
    }

    public Profiles profiles() {
        return this.profiles;
    }

    public void load() {
        this.profiles.boot();
    }

    public void defer() {
        if (!this.applying) {
            JsonObject jsonobject;
            try {
                jsonobject = this.snapshot();
            } catch (Throwable throwable) {
                LOG.error("Snapshot failed", throwable);
                return;
            }

            ScheduledFuture<?> scheduledfuture = this.pending.getAndSet(null);
            if (scheduledfuture != null) {
                scheduledfuture.cancel(false);
            }

            ScheduledFuture<?> scheduledfuture1 = this.io.schedule(() -> {
                try {
                    this.profiles.writeActive(jsonobject);
                } catch (Throwable throwable1) {
                    LOG.error("Autosave write failed", throwable1);
                }
            }, 250L, TimeUnit.MILLISECONDS);
            this.pending.set(scheduledfuture1);
        }
    }

    public void flush() {
        ScheduledFuture<?> scheduledfuture = this.pending.getAndSet(null);
        if (scheduledfuture != null) {
            scheduledfuture.cancel(false);
        }

        if (!this.applying) {
            try {
                this.profiles.writeActive(this.snapshot());
            } catch (Throwable throwable) {
                LOG.error("Flush failed", throwable);
            }
        }
    }

    public boolean applying() {
        return this.applying;
    }

    public JsonObject snapshot() {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("v", 5);
        JsonObject jsonobject1 = new JsonObject();
        JsonObject jsonobject2 = new JsonObject();

        for (Feature feature : this.bus.all()) {
            JsonObject jsonobject3 = new JsonObject();
            jsonobject3.addProperty("on", feature.on());
            JsonObject jsonobject4 = new JsonObject();

            for (Opt<?> opt : feature.opts()) {
                jsonobject4.add(opt.id(), opt.json());
            }

            jsonobject3.add("opts", jsonobject4);
            jsonobject1.add(feature.id(), jsonobject3);
            if (feature instanceof HudFeature hudfeature) {
                JsonObject jsonobject5 = new JsonObject();
                hudfeature.writeLayouts(jsonobject5);
                jsonobject2.add("hud", jsonobject5);
            }
        }

        GuiLayout.write(jsonobject2);
        jsonobject.add("features", jsonobject1);
        jsonobject.add("layout", jsonobject2);
        this.bus.find(GpsFeature.class).ifPresent(gps -> jsonobject.add("gps", gps.toJson()));
        return jsonobject;
    }

    public void apply(JsonObject root) {
        if (root != null) {
            this.applying = true;

            try {
                boolean flag = root.has("compact") && root.get("compact").getAsBoolean();
                if (root.has("features")) {
                    JsonObject jsonobject = root.getAsJsonObject("features");
                    if (flag) {
                        for (Feature feature : this.bus.all()) {
                            if (!"theme".equals(feature.id()) && !"config".equals(feature.id()) && !jsonobject.has(feature.id())) {
                                feature.set(false);

                                for (Opt<?> opt : feature.opts()) {
                                    opt.reset();
                                }
                            }
                        }
                    }

                    for (Feature feature2 : this.bus.all()) {
                        JsonObject jsonobject2 = jsonobject.getAsJsonObject(feature2.id());
                        if (jsonobject2 == null && "atmosphere".equals(feature2.id())) {
                            jsonobject2 = mergeAtmosphere(jsonobject);
                        }

                        if (jsonobject2 == null && "particles".equals(feature2.id())) {
                            jsonobject2 = jsonobject.getAsJsonObject("hit_fx");
                        }

                        if (jsonobject2 == null && "bright".equals(feature2.id())) {
                            jsonobject2 = jsonobject.getAsJsonObject("fullbright");
                        }

                        if (jsonobject2 == null && "hud".equals(feature2.id()) && !flag) {
                            jsonobject2 = new JsonObject();
                            jsonobject2.addProperty("on", true);
                        }

                        if (jsonobject2 != null) {
                            if (jsonobject2.has("on")) {
                                feature2.set(jsonobject2.get("on").getAsBoolean());
                            }

                            if (flag) {
                                for (Opt<?> opt1 : feature2.opts()) {
                                    opt1.reset();
                                }
                            }

                            if (jsonobject2.has("opts")) {
                                JsonObject jsonobject3 = jsonobject2.getAsJsonObject("opts");

                                for (Opt<?> opt2 : feature2.opts()) {
                                    JsonElement jsonelement = jsonobject3.get(opt2.id());
                                    if (jsonelement != null) {
                                        opt2.read(jsonelement);
                                    }
                                }
                            }
                        }
                    }
                }

                if (root.has("layout")) {
                    JsonObject jsonobject1 = root.getAsJsonObject("layout");
                    this.bus.find(HudFeature.class).ifPresent(hud -> {
                        if (jsonobject1.has("hud")) {
                            hud.readLayouts(jsonobject1.getAsJsonObject("hud"));
                        } else {
                            hud.readLayouts(jsonobject1);
                        }
                    });
                    GuiLayout.read(jsonobject1);
                    this.bus.find(GuiWindowsFeature.class).ifPresent(GuiWindowsFeature::pullFromLayout);
                }

                if (root.has("gps")) {
                    this.bus.find(GpsFeature.class).ifPresent(gps -> gps.fromJson(root.get("gps")));
                }

                this.bus.syncAllRuntime();

                for (Feature feature1 : this.bus.all()) {
                    feature1.poke();
                }
            } finally {
                this.applying = false;
            }

            try {
                this.profiles.writeActive(this.snapshot());
            } catch (Throwable throwable) {
                LOG.error("Post-apply flush failed", throwable);
            }
        }
    }

    public static Gson gson() {
        return GSON;
    }

    private static JsonObject mergeAtmosphere(JsonObject feats) {
        boolean flag = false;
        JsonObject jsonobject = new JsonObject();

        for (String s : new String[]{"ambience", "custom_fog", "time_weather", "chunk_reveal"}) {
            if (feats.has(s)) {
                JsonObject jsonobject1 = feats.getAsJsonObject(s);
                if (jsonobject1 != null && jsonobject1.has("on") && jsonobject1.get("on").getAsBoolean()) {
                    flag = true;
                }

                if (jsonobject1 != null && jsonobject1.has("opts") && jsonobject1.get("opts").isJsonObject()) {
                    JsonObject jsonobject2 = jsonobject1.getAsJsonObject("opts");

                    for (String s1 : jsonobject2.keySet()) {
                        if ("seconds".equals(s1) && !jsonobject.has("chunk_fade")) {
                            jsonobject.add("chunk_fade", jsonobject2.get(s1));
                        } else if (!jsonobject.has(s1)) {
                            jsonobject.add(s1, jsonobject2.get(s1));
                        }
                    }
                }
            }
        }

        if (!flag && jsonobject.size() == 0) {
            return null;
        } else {
            JsonObject jsonobject3 = new JsonObject();
            jsonobject3.addProperty("on", flag);
            jsonobject3.add("opts", jsonobject);
            return jsonobject3;
        }
    }
}
