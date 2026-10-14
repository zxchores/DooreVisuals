package dev.doorevisuals.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.data.ClientPaths;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.draw.WorldGui;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.AllowChat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GpsFeature extends Feature implements Tick {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/GPS");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme_color", "\u0426\u0432\u0435\u0442 \u0438\u0437 \u0442\u0435\u043c\u044b", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442", -12533600).visibleWhen(() -> !(Boolean)this.themeColor.get()));
    private final Opt.Flag worldMark = this.opt(new Opt.Flag("world", "\u041c\u0435\u0442\u043a\u0430 \u0432 \u043c\u0438\u0440\u0435", true));
    private final Opt.Num height = this.opt(new Opt.Num("height", "\u0412\u044b\u0441\u043e\u0442\u0430 \u043c\u0430\u044f\u043a\u0430", 48.0, 8.0, 96.0, 1.0));
    private final Opt.Num arrowOff = this.opt(
        new Opt.Num("arrow_off", "\u0421\u043c\u0435\u0449\u0435\u043d\u0438\u0435 \u0441\u0442\u0440\u0435\u043b\u043a\u0438", 20.0, 10.0, 64.0, 1.0)
    );
    private final Opt.Text activeName = this.opt(new Opt.Text("active", "\u0410\u043a\u0442\u0438\u0432\u043d\u0430\u044f \u0442\u043e\u0447\u043a\u0430", ""));
    private final Opt.Key place = this.opt(new Opt.Key("place", "\u041f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043c\u0435\u0442\u043a\u0443", -1));
    private boolean placeWas;
    private final Path path = ClientPaths.gps();
    private final List<GpsFeature.Marker> markers = new CopyOnWriteArrayList<>();
    private boolean bound;

    public GpsFeature() {
        super(
            "gps",
            "GPS",
            "\u0421\u0442\u0440\u0435\u043b\u043a\u0430 \u0438 \u0434\u0438\u0441\u0442\u0430\u043d\u0446\u0438\u044f \u0432 \u043c\u0438\u0440\u0435. .gps help",
            Category.WORLD,
            true
        );
    }

    public void bindChat() {
        if (!this.bound) {
            this.bound = true;
            this.loadFile();
            ClientSendMessageEvents.ALLOW_CHAT.register((AllowChat)message -> !this.on() ? true : !this.handle(message));
        }
    }

    public List<GpsFeature.Marker> markers() {
        return List.copyOf(this.markers);
    }

    public String activeMarkerName() {
        return this.activeName.get() == null ? "" : (String)this.activeName.get();
    }

    public void setActive(String name) {
        this.activeName.set(name == null ? "" : name);
        this.poke();
    }

    public void addPoint(String name, double x, double y, double z, String dimension) {
        this.addPoint(name, x, y, z, dimension, "pin");
    }

    public void addPoint(String name, double x, double y, double z, String dimension, String kind) {
        String s = name != null && !name.isBlank() ? name.trim() : "gps-" + (this.markers.size() + 1);
        String s1 = dimension != null && !dimension.isBlank() ? dimension : "minecraft:overworld";
        String s2 = kind != null && !kind.isBlank() ? kind : "pin";
        this.upsert(new GpsFeature.Marker(UUID.randomUUID().toString(), s, x, y, z, s1, s2));
        this.activeName.set(s);
        this.persist();
    }

    public void addDeath(double x, double y, double z, String dimension) {
        this.addPoint("\u0421\u043c\u0435\u0440\u0442\u044c", x, y, z, dimension, "death");
    }

    public void placeAtCrosshair() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.player != null) {
            double d0 = minecraftclient.player.getX();
            double d1 = minecraftclient.player.getY();
            double d2 = minecraftclient.player.getZ();
            HitResult hitresult = minecraftclient.crosshairTarget;
            if (hitresult instanceof BlockHitResult blockhitresult && hitresult.getType() == Type.BLOCK) {
                d0 = blockhitresult.getBlockPos().getX() + 0.5;
                d1 = blockhitresult.getBlockPos().getY() + 1;
                d2 = blockhitresult.getBlockPos().getZ() + 0.5;
            }

            this.addPoint("\u041c\u0435\u0442\u043a\u0430", d0, d1, d2, dimId(minecraftclient), "pin");
        }
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on() && mc.player != null && mc.currentScreen == null) {
            int i = (Integer)this.place.get();
            if (i > 0 && i != -1) {
                boolean flag = GLFW.glfwGetKey(mc.getWindow().getHandle(), i) == 1;
                if (flag && !this.placeWas) {
                    this.placeAtCrosshair();
                }

                this.placeWas = flag;
            } else {
                this.placeWas = false;
            }
        } else {
            this.placeWas = false;
        }
    }

    public void removeNamed(String name) {
        if (name != null && !name.isBlank()) {
            boolean flag = this.markers.removeIf(m -> m.name.equalsIgnoreCase(name));
            if (flag) {
                if (name.equalsIgnoreCase((String)this.activeName.get())) {
                    this.activeName.set("");
                }

                this.persist();
                this.poke();
            }
        }
    }

    public JsonObject toJson() {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("active", (String)this.activeName.get());
        JsonArray jsonarray = new JsonArray();

        for (GpsFeature.Marker gpsfeature$marker : this.markers) {
            jsonarray.add(gpsfeature$marker.toJson());
        }

        jsonobject.add("markers", jsonarray);
        return jsonobject;
    }

    public void fromJson(JsonElement el) {
        this.markers.clear();
        if (el != null && el.isJsonObject()) {
            JsonObject jsonobject = el.getAsJsonObject();
            if (jsonobject.has("active")) {
                this.activeName.set(jsonobject.get("active").getAsString());
            }

            JsonArray jsonarray = jsonobject.has("markers")
                ? jsonobject.getAsJsonArray("markers")
                : (jsonobject.has("list") ? jsonobject.getAsJsonArray("list") : null);
            if (jsonarray != null) {
                for (JsonElement jsonelement : jsonarray) {
                    GpsFeature.Marker gpsfeature$marker = GpsFeature.Marker.fromJson(jsonelement);
                    if (gpsfeature$marker != null) {
                        this.markers.add(gpsfeature$marker);
                    }
                }
            }

            this.writeFileQuiet();
        }
    }

    public void draw(WorldRenderContext ctx) {
        if ((Boolean)this.worldMark.get() && !this.markers.isEmpty()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            String s = dimId(minecraftclient);
            GpsFeature.Marker gpsfeature$marker = this.resolveTarget(minecraftclient);
            int i = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.color.get();
            float f = this.height.f();

            for (GpsFeature.Marker gpsfeature$marker1 : this.markers) {
                if (sameDim(gpsfeature$marker1, s)) {
                    boolean flag = gpsfeature$marker != null && gpsfeature$marker.id.equals(gpsfeature$marker1.id);
                    float f1 = flag ? 0.55F : 0.22F;
                    Mesh.cylinder(
                        ctx, gpsfeature$marker1.x, gpsfeature$marker1.y, gpsfeature$marker1.z, flag ? 0.4 : 0.28, f, 20, Mesh.alpha(i, f1), Types.glow()
                    );
                    Mesh.ring(
                        ctx, gpsfeature$marker1.x, gpsfeature$marker1.y + 0.05, gpsfeature$marker1.z, flag ? 1.0 : 0.7, 28, Mesh.alpha(i, flag ? 0.95F : 0.5F)
                    );
                    if (flag) {
                        Mesh.disc(ctx, gpsfeature$marker1.x, gpsfeature$marker1.y + f, gpsfeature$marker1.z, 0.1, 0.75, 24, Mesh.alpha(i, 0.6F), Types.glow());
                    }
                }
            }
        }
    }

    public void paintHud(DrawContext g) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.player != null && !this.markers.isEmpty() && !minecraftclient.options.hudHidden) {
            String s = dimId(minecraftclient);
            Vec3d vec3d = minecraftclient.player.getCameraPosVec(minecraftclient.getRenderTickCounter().getTickProgress(false));
            Ui.push();

            for (GpsFeature.Marker gpsfeature$marker : this.markers) {
                if (sameDim(gpsfeature$marker, s)) {
                    double d0 = gpsfeature$marker.x - vec3d.x;
                    double d2 = gpsfeature$marker.y - vec3d.y;
                    double d4 = gpsfeature$marker.z - vec3d.z;
                    double d6 = Math.sqrt(d0 * d0 + d2 * d2 + d4 * d4);
                    if (!(d6 < 1.2)) {
                        float[] afloat = WorldGui.project(gpsfeature$marker.x, gpsfeature$marker.y + 1.15, gpsfeature$marker.z);
                        if (afloat != null) {
                            this.paintPill(g, afloat[0], afloat[1], gpsfeature$marker, d6);
                        }
                    }
                }
            }

            GpsFeature.Marker gpsfeature$marker1 = this.resolveTarget(minecraftclient);
            if (gpsfeature$marker1 != null && sameDim(gpsfeature$marker1, s)) {
                double d7 = gpsfeature$marker1.x - vec3d.x;
                double d1 = gpsfeature$marker1.y - vec3d.y;
                double d3 = gpsfeature$marker1.z - vec3d.z;
                double d5 = Math.sqrt(d7 * d7 + d1 * d1 + d3 * d3);
                if (d5 >= 1.2) {
                    float f = (float)(MathHelper.atan2(d3, d7) * (180.0 / Math.PI)) - 90.0F;
                    float f5 = MathHelper.wrapDegrees(f - minecraftclient.player.getYaw());
                    float f1 = g.getScaledWindowWidth() * 0.5F;
                    float f2 = g.getScaledWindowHeight() * 0.5F;
                    float f3 = this.arrowOff.f();
                    float f4 = (float)Math.toRadians(f5);
                    int i = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.color.get();
                    drawArrow(f1 + MathHelper.sin(f4) * f3, f2 - MathHelper.cos(f4) * f3, f5, i);
                }
            }

            Ui.pop();
        }
    }

    private void paintPill(DrawContext g, float cx, float cy, GpsFeature.Marker m, double dist) {
        boolean flag = m.death();
        String s = flag ? "\u0421\u043c\u0435\u0440\u0442\u044c" : "\u041c\u0435\u0442\u043a\u0430";
        String s1 = Math.round(dist) + " \u043c";
        String s2 = Math.round(m.x) + ", " + Math.round(m.y) + ", " + Math.round(m.z);
        float f = 118.0F;
        float f1 = 36.0F;
        float f2 = cx - f * 0.5F;
        float f3 = cy - f1 - 8.0F;
        Paint.box(g, f2, f3, f, f1, Theme.alpha(329483, 220), 10.0F);
        Nvg.circle(f2 + 16.0F, f3 + f1 * 0.5F, 9.0F, Theme.alpha(flag ? 14830411 : Theme.ACCENT, 40));
        Nvg.circle(f2 + 16.0F, f3 + f1 * 0.5F, 4.2F, flag ? -2074022 : Theme.ACCENT);
        Paint.text(g, s, f2 + 30.0F, f3 + 7.0F, Theme.TEXT, 7.2F);
        Paint.textR(g, s1, f2 + f - 8.0F, f3 + 7.0F, Theme.ACCENT_HOT, 7.2F);
        Paint.text(g, s2, f2 + 30.0F, f3 + 20.0F, Theme.MUTED, 5.6F);
    }

    private static void drawArrow(float cx, float cy, float deg, int color) {
        float f = (float)Math.toRadians(deg);
        float f1 = 5.5F;
        float f2 = cx + MathHelper.sin(f) * f1;
        float f3 = cy - MathHelper.cos(f) * f1;
        float f4 = cx - MathHelper.sin(f) * (f1 * 0.55F);
        float f5 = cy + MathHelper.cos(f) * (f1 * 0.55F);
        float f6 = MathHelper.cos(f) * 3.6F;
        float f7 = MathHelper.sin(f) * 3.6F;
        Nvg.triangle(f2, f3, f4 - f6, f5 - f7, f4 + f6, f5 + f7, color);
    }

    private GpsFeature.Marker resolveTarget(MinecraftClient mc) {
        String s = dimId(mc);
        String s1 = (String)this.activeName.get();
        if (s1 != null && !s1.isBlank()) {
            for (GpsFeature.Marker gpsfeature$marker : this.markers) {
                if (gpsfeature$marker.name.equalsIgnoreCase(s1) && sameDim(gpsfeature$marker, s)) {
                    return gpsfeature$marker;
                }
            }
        }

        return this.nearest(mc, s);
    }

    private GpsFeature.Marker nearest(MinecraftClient mc, String dim) {
        GpsFeature.Marker gpsfeature$marker = null;
        double d0 = Double.MAX_VALUE;
        double d1 = mc.player.getX();
        double d2 = mc.player.getY();
        double d3 = mc.player.getZ();

        for (GpsFeature.Marker gpsfeature$marker1 : this.markers) {
            if (sameDim(gpsfeature$marker1, dim)) {
                double d4 = (gpsfeature$marker1.x - d1) * (gpsfeature$marker1.x - d1)
                    + (gpsfeature$marker1.y - d2) * (gpsfeature$marker1.y - d2)
                    + (gpsfeature$marker1.z - d3) * (gpsfeature$marker1.z - d3);
                if (d4 < d0) {
                    d0 = d4;
                    gpsfeature$marker = gpsfeature$marker1;
                }
            }
        }

        return gpsfeature$marker;
    }

    private boolean handle(String message) {
        if (message != null && message.startsWith(".gps")) {
            List<String> list = tokenize(message.trim());
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (list.size() == 1 || list.get(1).equalsIgnoreCase("help")) {
                chat(minecraftclient, ".gps add/here/list/del/goto | .gps <x> <z> | .gps off");
                return true;
            } else if (number(list.get(1))) {
                this.placeCoords(minecraftclient, list, 1);
                return true;
            } else {
                String s = list.get(1).toLowerCase(Locale.ROOT);
                switch (s) {
                    case "here":
                        if (minecraftclient.player == null) {
                            return true;
                        }

                        String s6 = list.size() >= 3 ? joinFrom(list, 2) : "here-" + (this.markers.size() + 1);
                        this.upsert(
                            new GpsFeature.Marker(
                                UUID.randomUUID().toString(),
                                s6,
                                minecraftclient.player.getX(),
                                minecraftclient.player.getY(),
                                minecraftclient.player.getZ(),
                                dimId(minecraftclient),
                                "pin"
                            )
                        );
                        this.activeName.set(s6);
                        this.persist();
                        chat(minecraftclient, "GPS +" + s6);
                        break;
                    case "add":
                        if (list.size() < 4) {
                            chat(minecraftclient, ".gps add <x> <z> [name] | .gps add <x> <y> <z> [name]");
                            return true;
                        }

                        try {
                            double d0 = Double.parseDouble(list.get(2));
                            double d1 = Double.parseDouble(list.get(3));
                            GpsFeature.Marker gpsfeature$marker3;
                            if (list.size() >= 5 && number(list.get(4))) {
                                gpsfeature$marker3 = new GpsFeature.Marker(
                                    UUID.randomUUID().toString(),
                                    this.nameFrom(list, 5),
                                    d0,
                                    d1,
                                    Double.parseDouble(list.get(4)),
                                    dimId(minecraftclient),
                                    "pin"
                                );
                            } else {
                                double d2 = minecraftclient.player != null ? minecraftclient.player.getY() : 64.0;
                                gpsfeature$marker3 = new GpsFeature.Marker(
                                    UUID.randomUUID().toString(), this.nameFrom(list, 4), d0, d2, d1, dimId(minecraftclient), "pin"
                                );
                            }

                            this.upsert(gpsfeature$marker3);
                            this.activeName.set(gpsfeature$marker3.name);
                            this.persist();
                            chat(minecraftclient, "GPS +" + gpsfeature$marker3.name);
                        } catch (NumberFormatException numberformatexception) {
                            chat(
                                minecraftclient,
                                "\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0435 \u043a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442\u044b."
                            );
                        }
                        break;
                    case "list":
                        String s5 = dimId(minecraftclient);
                        List<GpsFeature.Marker> list2 = this.markers.stream().filter(m -> sameDim(m, s5)).toList();
                        if (list2.isEmpty()) {
                            chat(
                                minecraftclient,
                                "\u041d\u0435\u0442 \u0442\u043e\u0447\u0435\u043a \u0432 \u044d\u0442\u043e\u043c \u0438\u0437\u043c\u0435\u0440\u0435\u043d\u0438\u0438."
                            );
                        } else {
                            int j = 1;

                            for (GpsFeature.Marker gpsfeature$marker2 : list2) {
                                String s8 = gpsfeature$marker2.name.equalsIgnoreCase((String)this.activeName.get()) ? " \u25cf" : "";
                                chat(
                                    minecraftclient,
                                    j++
                                        + ". "
                                        + gpsfeature$marker2.name
                                        + s8
                                        + " \u2014 "
                                        + fmt(gpsfeature$marker2.x)
                                        + " "
                                        + fmt(gpsfeature$marker2.y)
                                        + " "
                                        + fmt(gpsfeature$marker2.z)
                                );
                            }
                        }
                        break;
                    case "goto":
                        if (list.size() < 3) {
                            chat(minecraftclient, ".gps goto <name>");
                            return true;
                        }

                        String s4 = joinFrom(list, 2);
                        GpsFeature.Marker gpsfeature$marker1 = this.findByName(s4);
                        if (gpsfeature$marker1 == null) {
                            chat(minecraftclient, "\u041d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e.");
                        } else {
                            this.activeName.set(gpsfeature$marker1.name);
                            this.persist();
                            chat(minecraftclient, "\u0410\u043a\u0442\u0438\u0432\u043d\u0430: " + gpsfeature$marker1.name);
                        }
                        break;
                    case "del":
                    case "remove":
                    case "rm":
                        if (list.size() < 3) {
                            chat(minecraftclient, ".gps del <name|#>");
                            return true;
                        }

                        String s3 = list.get(2);
                        boolean flag = false;
                        if (s3.matches("\\d+")) {
                            String s2 = dimId(minecraftclient);
                            List<GpsFeature.Marker> list1 = this.markers.stream().filter(m -> sameDim(m, s2)).toList();
                            int i = Integer.parseInt(s3) - 1;
                            if (i >= 0 && i < list1.size()) {
                                GpsFeature.Marker gpsfeature$marker = list1.get(i);
                                this.markers.removeIf(m -> m.id.equals(gpsfeature$marker.id));
                                if (gpsfeature$marker.name.equalsIgnoreCase((String)this.activeName.get())) {
                                    this.activeName.set("");
                                }

                                chat(minecraftclient, "\u0423\u0434\u0430\u043b\u0435\u043d\u043e " + gpsfeature$marker.name);
                                flag = true;
                            }
                        } else {
                            String s7 = joinFrom(list, 2);
                            flag = this.markers.removeIf(m -> m.name.equalsIgnoreCase(s7));
                            if (flag && s7.equalsIgnoreCase((String)this.activeName.get())) {
                                this.activeName.set("");
                            }

                            chat(
                                minecraftclient,
                                flag ? "\u0423\u0434\u0430\u043b\u0435\u043d\u043e " + s7 : "\u041d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e."
                            );
                        }

                        if (flag) {
                            this.persist();
                        } else if (s3.matches("\\d+")) {
                            chat(minecraftclient, "\u041d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e.");
                        }
                        break;
                    case "off":
                        this.markers.clear();
                        this.activeName.set("");
                        this.persist();
                        chat(
                            minecraftclient,
                            "GPS \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d \u2014 \u0432\u0441\u0435 \u043c\u0435\u0442\u043a\u0438 \u0443\u0434\u0430\u043b\u0435\u043d\u044b."
                        );
                        break;
                    case "clear":
                        String s1 = dimId(minecraftclient);
                        this.markers.removeIf(m -> sameDim(m, s1));
                        this.activeName.set("");
                        this.persist();
                        chat(
                            minecraftclient,
                            "GPS \u043e\u0447\u0438\u0449\u0435\u043d (\u044d\u0442\u043e \u0438\u0437\u043c\u0435\u0440\u0435\u043d\u0438\u0435)."
                        );
                        break;
                    default:
                        chat(minecraftclient, "\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u043e. .gps help");
                }

                return true;
            }
        } else {
            return false;
        }
    }

    private void placeCoords(MinecraftClient mc, List<String> parts, int start) {
        if (parts.size() < start + 2) {
            chat(mc, ".gps <x> <z> | .gps <x> <y> <z>");
        } else {
            try {
                double d0 = Double.parseDouble(parts.get(start));
                double d1 = Double.parseDouble(parts.get(start + 1));
                GpsFeature.Marker gpsfeature$marker;
                if (parts.size() > start + 2 && number(parts.get(start + 2))) {
                    gpsfeature$marker = new GpsFeature.Marker(
                        UUID.randomUUID().toString(), this.nameFrom(parts, start + 3), d0, d1, Double.parseDouble(parts.get(start + 2)), dimId(mc), "pin"
                    );
                } else {
                    double d2 = mc.player != null ? mc.player.getY() : 64.0;
                    gpsfeature$marker = new GpsFeature.Marker(UUID.randomUUID().toString(), this.nameFrom(parts, start + 2), d0, d2, d1, dimId(mc), "pin");
                }

                this.upsert(gpsfeature$marker);
                this.activeName.set(gpsfeature$marker.name);
                this.persist();
                chat(mc, "GPS +" + gpsfeature$marker.name);
            } catch (NumberFormatException numberformatexception) {
                chat(mc, "\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0435 \u043a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442\u044b.");
            }
        }
    }

    private void upsert(GpsFeature.Marker marker) {
        this.markers.removeIf(m -> m.name.equalsIgnoreCase(marker.name) && sameDim(m, marker.dimension));
        this.markers.add(marker);
    }

    private GpsFeature.Marker findByName(String name) {
        for (GpsFeature.Marker gpsfeature$marker : this.markers) {
            if (gpsfeature$marker.name.equalsIgnoreCase(name)) {
                return gpsfeature$marker;
            }
        }

        return null;
    }

    private void persist() {
        this.writeFileQuiet();
        if (App.live() && !App.save().applying()) {
            App.save().defer();
        }
    }

    private void loadFile() {
        this.markers.clear();
        if (Files.exists(this.path)) {
            try {
                JsonElement jsonelement = JsonParser.parseString(Files.readString(this.path, StandardCharsets.UTF_8));
                if (jsonelement.isJsonArray()) {
                    for (JsonElement jsonelement1 : jsonelement.getAsJsonArray()) {
                        GpsFeature.Marker gpsfeature$marker = GpsFeature.Marker.fromJson(jsonelement1);
                        if (gpsfeature$marker != null) {
                            this.markers.add(gpsfeature$marker);
                        }
                    }
                } else if (jsonelement.isJsonObject()) {
                    this.fromJson(jsonelement);
                }
            } catch (Exception exception) {
                LOG.warn("GPS load failed", exception);
            }
        }
    }

    private void writeFileQuiet() {
        try {
            Files.createDirectories(this.path.getParent());
            JsonObject jsonobject = this.toJson();
            Path path = this.path.resolveSibling("gps.tmp");
            Files.writeString(path, GSON.toJson(jsonobject), StandardCharsets.UTF_8);

            try {
                Files.move(path, this.path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception exception) {
                Files.move(path, this.path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception1) {
            LOG.warn("GPS save failed", exception1);
        }
    }

    private static String dimId(MinecraftClient mc) {
        if (mc.world == null) {
            return "minecraft:overworld";
        } else {
            RegistryKey<World> registrykey = mc.world.getRegistryKey();
            return registrykey.getValue().toString();
        }
    }

    private static boolean sameDim(GpsFeature.Marker m, String dim) {
        return m.dimension == null || m.dimension.isBlank() || m.dimension.equals(dim);
    }

    private String nameFrom(List<String> parts, int start) {
        return start >= parts.size() ? "gps-" + (this.markers.size() + 1) : joinFrom(parts, start);
    }

    private static String joinFrom(List<String> parts, int start) {
        StringBuilder stringbuilder = new StringBuilder();

        for (int i = start; i < parts.size(); i++) {
            if (i > start) {
                stringbuilder.append(' ');
            }

            stringbuilder.append(parts.get(i));
        }

        return stringbuilder.toString().replace(' ', '-');
    }

    private static List<String> tokenize(String raw) {
        List<String> list = new ArrayList<>();
        StringBuilder stringbuilder = new StringBuilder();
        boolean flag = false;

        for (int i = 0; i < raw.length(); i++) {
            char c0 = raw.charAt(i);
            if (c0 == '"') {
                flag = !flag;
            } else if (flag || !Character.isWhitespace(c0)) {
                stringbuilder.append(c0);
            } else if (!stringbuilder.isEmpty()) {
                list.add(stringbuilder.toString());
                stringbuilder.setLength(0);
            }
        }

        if (!stringbuilder.isEmpty()) {
            list.add(stringbuilder.toString());
        }

        return list;
    }

    private static boolean number(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static void chat(MinecraftClient mc, String text) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("\u00a73[GPS]\u00a7r " + text), false);
        }
    }

    public record Marker(String id, String name, double x, double y, double z, String dimension, String kind) {
        boolean death() {
            return "death".equalsIgnoreCase(this.kind);
        }

        JsonObject toJson() {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("id", this.id);
            jsonobject.addProperty("name", this.name);
            jsonobject.addProperty("x", this.x);
            jsonobject.addProperty("y", this.y);
            jsonobject.addProperty("z", this.z);
            jsonobject.addProperty("dimension", this.dimension == null ? "minecraft:overworld" : this.dimension);
            jsonobject.addProperty("kind", this.kind != null && !this.kind.isBlank() ? this.kind : "pin");
            return jsonobject;
        }

        static GpsFeature.Marker fromJson(JsonElement el) {
            if (el != null && el.isJsonObject()) {
                JsonObject jsonobject = el.getAsJsonObject();
                if (jsonobject.has("name") && jsonobject.has("x") && jsonobject.has("z")) {
                    String s = jsonobject.has("id") ? jsonobject.get("id").getAsString() : UUID.randomUUID().toString();
                    String s1 = jsonobject.has("dimension") ? jsonobject.get("dimension").getAsString() : "minecraft:overworld";
                    String s2 = jsonobject.has("kind") ? jsonobject.get("kind").getAsString() : "pin";
                    return new GpsFeature.Marker(
                        s,
                        jsonobject.get("name").getAsString(),
                        jsonobject.get("x").getAsDouble(),
                        jsonobject.has("y") ? jsonobject.get("y").getAsDouble() : 64.0,
                        jsonobject.get("z").getAsDouble(),
                        s1,
                        s2
                    );
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }
}
