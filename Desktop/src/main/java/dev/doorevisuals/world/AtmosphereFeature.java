package dev.doorevisuals.world;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.draw.VisualQuality;
import dev.doorevisuals.tools.ThemeFeature;
import dev.doorevisuals.tools.TimeWeatherFeature;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class AtmosphereFeature extends Feature implements Tick {
    private final Opt.Pick preset = this.opt(
        new Opt.Pick(
            "preset",
            "\u041f\u0440\u0435\u0441\u0435\u0442",
            "\u0412\u0430\u043d\u0438\u043b\u044c+",
            "\u041d\u043e\u0447\u044c",
            "\u0417\u0430\u043a\u0430\u0442",
            "\u0421\u0430\u043a\u0443\u0440\u0430",
            "\u0425\u043e\u043b\u043e\u0434",
            "\u041a\u0438\u0431\u0435\u0440",
            "\u0412\u0430\u043d\u0438\u043b\u044c+"
        )
    );
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme_color", "\u0426\u0432\u0435\u0442 \u0438\u0437 \u0442\u0435\u043c\u044b", false));
    private final Opt.Tint day = this.opt(new Opt.Tint("day", "\u0414\u0435\u043d\u044c", -12533600));
    private final Opt.Tint dawn = this.opt(new Opt.Tint("dawn", "\u0420\u0430\u0441\u0441\u0432\u0435\u0442", -29607));
    private final Opt.Tint dusk = this.opt(new Opt.Tint("dusk", "\u0417\u0430\u043a\u0430\u0442", -4236839));
    private final Opt.Num strength = this.opt(new Opt.Num("strength", "\u0421\u0438\u043b\u0430 \u0446\u0432\u0435\u0442\u0430", 0.45, 0.05, 1.0, 0.05));
    private final Opt.Flag blendTime = this.opt(new Opt.Flag("time_blend", "\u0420\u0430\u0441\u0441\u0432\u0435\u0442 / \u0437\u0430\u043a\u0430\u0442", true));
    private final Opt.Pick timePreset = this.opt(
        new Opt.Pick(
            "time_preset",
            "\u0412\u0440\u0435\u043c\u044f",
            "\u0412\u0430\u043d\u0438\u043b\u044c",
            "\u0412\u0430\u043d\u0438\u043b\u044c",
            "\u0420\u0430\u0441\u0441\u0432\u0435\u0442",
            "\u041f\u043e\u043b\u0434\u0435\u043d\u044c",
            "\u0417\u0430\u043a\u0430\u0442",
            "\u041f\u043e\u043b\u043d\u043e\u0447\u044c"
        )
    );
    private final Opt.Flag lockTime = this.opt(
        new Opt.Flag("lock_time", "\u0417\u0430\u0444\u0438\u043a\u0441\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u0432\u0440\u0435\u043c\u044f", false)
    );
    private final Opt.Num timeValue = this.opt(
        new Opt.Num("time", "\u0422\u0438\u043a\u0438 \u0434\u043d\u044f", 6000.0, 0.0, 24000.0, 100.0).visibleWhen(this.lockTime::get)
    );
    private final Opt.Num timeSpeed = this.opt(
        new Opt.Num("time_speed", "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0432\u0440\u0435\u043c\u0435\u043d\u0438", 1.0, 0.0, 10.0, 0.25)
    );
    private final Opt.Pick weather = this.opt(
        new Opt.Pick(
            "weather",
            "\u041f\u043e\u0433\u043e\u0434\u0430",
            "\u0412\u0430\u043d\u0438\u043b\u044c",
            "\u0412\u0430\u043d\u0438\u043b\u044c",
            "\u042f\u0441\u043d\u043e",
            "\u0414\u043e\u0436\u0434\u044c",
            "\u0413\u0440\u043e\u0437\u0430"
        )
    );
    private final Opt.Num fogDensity = this.opt(new Opt.Num("fog_density", "\u0422\u0443\u043c\u0430\u043d", 1.05, 0.3, 2.5, 0.05));
    private final Opt.Num fogEnd = this.opt(
        new Opt.Num("fog_end", "\u0414\u0430\u043b\u044c\u043d\u043e\u0441\u0442\u044c \u0442\u0443\u043c\u0430\u043d\u0430", 0.78, 0.25, 1.5, 0.05)
    );
    private final Opt.Flag clouds = this.opt(new Opt.Flag("clouds", "\u041e\u0431\u043b\u0430\u043a\u0430", true));
    private final Opt.Flag aurora = this.opt(new Opt.Flag("aurora", "\u0410\u0432\u0440\u043e\u0440\u0430", false));
    private final Opt.Flag rainFx = this.opt(new Opt.Flag("rain_fx", "\u0414\u043e\u0436\u0434\u044c FX", true));
    private final Opt.Flag vanillaSky = this.opt(
        new Opt.Flag("vanilla_sky", "\u0412\u0430\u043d\u0438\u043b\u044c\u043d\u043e\u0435 \u043d\u0435\u0431\u043e", false)
    );
    private final Opt.Pick quality = this.opt(
        new Opt.Pick(
            "quality",
            "\u041a\u0430\u0447\u0435\u0441\u0442\u0432\u043e FX",
            "\u0412\u044b\u0441\u043e\u043a\u043e\u0435",
            "\u041d\u0438\u0437\u043a\u043e\u0435",
            "\u0421\u0440\u0435\u0434\u043d\u0435\u0435",
            "\u0412\u044b\u0441\u043e\u043a\u043e\u0435"
        )
    );
    private final Opt.Num chunkFade = this.opt(
        new Opt.Num("chunk_fade", "\u041f\u043e\u044f\u0432\u043b\u0435\u043d\u0438\u0435 \u0447\u0430\u043d\u043a\u043e\u0432", 0.55, 0.0, 2.5, 0.05)
    );
    private String lastPreset = "";
    private String lastQuality = "";
    private Double prevChunkFade;

    public AtmosphereFeature() {
        super(
            "atmosphere",
            "Atmosphere",
            "\u041d\u0435\u0431\u043e, \u0442\u0443\u043c\u0430\u043d, \u0432\u0440\u0435\u043c\u044f, \u043e\u0431\u043b\u0430\u043a\u0430, \u0430\u0432\u0440\u043e\u0440\u0430 \u0438 \u043f\u043e\u044f\u0432\u043b\u0435\u043d\u0438\u0435 \u0447\u0430\u043d\u043a\u043e\u0432",
            Category.WORLD,
            false
        );
        this.lastPreset = (String)this.preset.get();
        this.lastQuality = (String)this.quality.get();
    }

    public static boolean hideVanillaSky() {
        return App.features().find(AtmosphereFeature.class).map(f -> f.on() && !(Boolean)f.vanillaSky.get()).orElse(false);
    }

    public static boolean hideVanillaClouds() {
        return App.features().find(AtmosphereFeature.class).map(f -> f.on() && (Boolean)f.clouds.get() && !(Boolean)f.vanillaSky.get()).orElse(false);
    }

    public static boolean hideVanillaRain() {
        return App.features().find(AtmosphereFeature.class).map(f -> f.on() && (Boolean)f.rainFx.get()).orElse(false);
    }

    @Override
    protected void enable() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (this.prevChunkFade == null) {
            this.prevChunkFade = (Double)minecraftclient.options.getChunkFade().getValue();
        }

        this.push();
    }

    @Override
    protected void disable() {
        AmbienceFeature.applyLive(false, 0.0F, false, 0, 0, 0, "\u0412\u0430\u043d\u0438\u043b\u044c", "\u0412\u0430\u043d\u0438\u043b\u044c");
        FogFeature.applyLive(false, 1.0F, 0.0F, 1.0F, 0.0F, 0);
        TimeWeatherFeature.applyLive(false, false, 6000, 1.0F, "default");
        this.restoreChunkFade();
    }

    @Override
    public void poke() {
        super.poke();
        this.push();
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on()) {
            if (!((String)this.preset.get()).equals(this.lastPreset)) {
                this.applyPreset((String)this.preset.get());
                this.lastPreset = (String)this.preset.get();
            }

            if (!((String)this.quality.get()).equals(this.lastQuality)) {
                this.lastQuality = (String)this.quality.get();
            }

            this.push();
            TimeWeatherFeature.advance();
        }
    }

    public void draw(WorldRenderContext ctx, float tickDelta) {
        if (this.on()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            ClientPlayerEntity clientplayerentity = minecraftclient.player;
            if (clientplayerentity != null && minecraftclient.world != null) {
                Vec3d vec3d = clientplayerentity.getCameraPosVec(tickDelta);
                int i = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.day.get();
                AtmosphereFeature.Sky atmospherefeature$sky = this.skyOf((String)this.preset.get(), i);
                if (!(Boolean)this.vanillaSky.get()) {
                    this.drawSky(ctx, vec3d, atmospherefeature$sky);
                    if ((Boolean)this.clouds.get()) {
                        this.drawClouds(ctx, vec3d, atmospherefeature$sky);
                    }
                }

                if ((Boolean)this.aurora.get() && VisualQuality.level() != VisualQuality.Level.LOW) {
                    this.drawAurora(ctx, vec3d, atmospherefeature$sky);
                }

                if ((Boolean)this.rainFx.get()) {
                    this.drawRain(ctx, minecraftclient.world, vec3d, i);
                }
            }
        }
    }

    private void push() {
        if (!this.on()) {
            this.disable();
        } else {
            VisualQuality.setLabel((String)this.quality.get());
            this.pushChunkFade();
            int i = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.day.get();
            AmbienceFeature.applyLive(
                true,
                this.strength.f(),
                (Boolean)this.blendTime.get(),
                i,
                (Integer)this.dawn.get(),
                (Integer)this.dusk.get(),
                (String)this.timePreset.get(),
                this.visualWeather()
            );
            FogFeature.applyLive(true, this.fogDensity.f(), 0.22F, this.fogEnd.f(), 0.88F, i);
            TimeWeatherFeature.applyLive(
                true, (Boolean)this.lockTime.get() || this.lockedByPreset(), this.lockedTime(), this.timeSpeed.f(), this.mixinWeather()
            );
        }
    }

    private void pushChunkFade() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        double d0 = (Double)this.chunkFade.get();
        Double d1 = (Double)minecraftclient.options.getChunkFade().getValue();
        if (d1 == null || Math.abs(d1 - d0) > 0.001) {
            minecraftclient.options.getChunkFade().setValue(d0);
        }
    }

    private void restoreChunkFade() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (this.prevChunkFade != null) {
            minecraftclient.options.getChunkFade().setValue(this.prevChunkFade);
            this.prevChunkFade = null;
        }
    }

    private boolean lockedByPreset() {
        return !"\u0412\u0430\u043d\u0438\u043b\u044c".equals(this.timePreset.get()) && !"\u0412\u0430\u043d\u0438\u043b\u044c+".equals(this.preset.get());
    }

    private int lockedTime() {
        if ((Boolean)this.lockTime.get()) {
            return this.timeValue.i();
        } else {
            String s = (String)this.timePreset.get();

            return switch (s) {
                case "\u0420\u0430\u0441\u0441\u0432\u0435\u0442" -> 1000;
                case "\u041f\u043e\u043b\u0434\u0435\u043d\u044c" -> 6000;
                case "\u0417\u0430\u043a\u0430\u0442" -> 12500;
                case "\u041f\u043e\u043b\u043d\u043e\u0447\u044c" -> 18000;
                default -> 6000;
            };
        }
    }

    private String visualWeather() {
        String s = (String)this.weather.get();

        return switch (s) {
            case "\u042f\u0441\u043d\u043e" -> "\u042f\u0441\u043d\u043e";
            case "\u0414\u043e\u0436\u0434\u044c", "\u0413\u0440\u043e\u0437\u0430" -> "\u0414\u043e\u0436\u0434\u044c";
            default -> "\u0412\u0430\u043d\u0438\u043b\u044c";
        };
    }

    private String mixinWeather() {
        String s = (String)this.weather.get();

        return switch (s) {
            case "\u042f\u0441\u043d\u043e" -> "clear";
            case "\u0414\u043e\u0436\u0434\u044c" -> "rain";
            case "\u0413\u0440\u043e\u0437\u0430" -> "thunder";
            default -> "default";
        };
    }

    private void applyPreset(String label) {
        switch (label) {
            case "\u041d\u043e\u0447\u044c":
                this.day.set(-15062443);
                this.dawn.set(-12957056);
                this.dusk.set(-10863984);
                this.strength.set(0.72);
                this.timePreset.set("\u041f\u043e\u043b\u043d\u043e\u0447\u044c");
                this.weather.set("\u0412\u0430\u043d\u0438\u043b\u044c");
                this.fogDensity.set(1.35);
                this.fogEnd.set(0.58);
                this.aurora.set(true);
                this.clouds.set(true);
                break;
            case "\u0417\u0430\u043a\u0430\u0442":
                this.day.set(-30134);
                this.dawn.set(-16264);
                this.dusk.set(-4236839);
                this.strength.set(0.62);
                this.timePreset.set("\u0417\u0430\u043a\u0430\u0442");
                this.weather.set("\u0412\u0430\u043d\u0438\u043b\u044c");
                this.fogDensity.set(1.15);
                this.fogEnd.set(0.7);
                this.aurora.set(false);
                this.clouds.set(true);
                break;
            case "\u0421\u0430\u043a\u0443\u0440\u0430":
                this.day.set(-18491);
                this.dawn.set(-10528);
                this.dusk.set(-1533228);
                this.strength.set(0.48);
                this.timePreset.set("\u0420\u0430\u0441\u0441\u0432\u0435\u0442");
                this.weather.set("\u042f\u0441\u043d\u043e");
                this.fogDensity.set(0.95);
                this.fogEnd.set(0.85);
                this.aurora.set(false);
                this.clouds.set(true);
                break;
            case "\u0425\u043e\u043b\u043e\u0434":
                this.day.set(-4659969);
                this.dawn.set(-1510145);
                this.dusk.set(-8740664);
                this.strength.set(0.55);
                this.timePreset.set("\u041f\u043e\u043b\u0434\u0435\u043d\u044c");
                this.weather.set("\u042f\u0441\u043d\u043e");
                this.fogDensity.set(1.25);
                this.fogEnd.set(0.62);
                this.aurora.set(true);
                this.clouds.set(true);
                break;
            case "\u041a\u0438\u0431\u0435\u0440":
                this.day.set(-14622528);
                this.dawn.set(-8458284);
                this.dusk.set(-50476);
                this.strength.set(0.7);
                this.timePreset.set("\u041f\u043e\u043b\u043d\u043e\u0447\u044c");
                this.weather.set("\u042f\u0441\u043d\u043e");
                this.fogDensity.set(1.4);
                this.fogEnd.set(0.55);
                this.aurora.set(true);
                this.clouds.set(true);
                break;
            default:
                this.day.set(-12533600);
                this.dawn.set(-29607);
                this.dusk.set(-4236839);
                this.strength.set(0.28);
                this.timePreset.set("\u0412\u0430\u043d\u0438\u043b\u044c");
                this.weather.set("\u0412\u0430\u043d\u0438\u043b\u044c");
                this.fogDensity.set(0.9);
                this.fogEnd.set(0.92);
                this.aurora.set(false);
                this.clouds.set(true);
        }
    }

    private AtmosphereFeature.Sky skyOf(String label, int tint) {
        return switch (label) {
            case "\u041d\u043e\u0447\u044c" -> new AtmosphereFeature.Sky(
                -16315620, -15195064, -5944, -1511169, Theme.tintMul(-7692072, tint, 0.2F), -12779622, -8758017, false, true
            );
            case "\u0417\u0430\u043a\u0430\u0442" -> new AtmosphereFeature.Sky(
                -15068104, -38342, -16264, -7992, Theme.tintMul(-12112, tint, 0.35F), -30134, -4236839, true, false
            );
            case "\u0421\u0430\u043a\u0443\u0440\u0430" -> new AtmosphereFeature.Sky(
                -6368001, -15660, -3368, -5904, Theme.tintMul(-7958, tint, 0.4F), -18491, -1533228, true, false
            );
            case "\u0425\u043e\u043b\u043e\u0434" -> new AtmosphereFeature.Sky(
                -9520897, -1510145, -2336, -2560769, Theme.tintMul(-853249, tint, 0.25F), -8462081, -4659969, true, true
            );
            case "\u041a\u0438\u0431\u0435\u0440" -> new AtmosphereFeature.Sky(
                -16576488, -16238544, -14622528, -50476, Theme.tintMul(-15054264, tint, 0.35F), -14622528, -50476, false, true
            );
            default -> new AtmosphereFeature.Sky(-11884328, -3610369, -3896, -1510145, Theme.tintMul(-722177, tint, 0.22F), -12533600, -4236839, true, false);
        };
    }

    private void drawSky(WorldRenderContext ctx, Vec3d cam, AtmosphereFeature.Sky sky) {
        float f = dayPhase();
        double d0 = Math.cos((f - 0.25F) * Math.PI * 2.0);
        double d1 = f * Math.PI * 2.0;
        int i = VisualQuality.segs(28);
        Mesh.disc(ctx, cam.x, cam.y + 18.0, cam.z, 48.0, 175.0, i, Mesh.alpha(sky.horizon, 0.22F), Types.glow());
        Mesh.disc(ctx, cam.x, cam.y + 42.0, cam.z, 20.0, 150.0, Math.max(16, i - 4), Mesh.alpha(sky.horizon, 0.16F), Types.glow());
        Mesh.disc(ctx, cam.x, cam.y + 78.0, cam.z, 0.0, 118.0, Math.max(14, i - 6), Mesh.alpha(sky.zenith, 0.2F), Types.glow());
        Mesh.orb(ctx, cam.x, cam.y + 132.0, cam.z, 92.0F, Mesh.alpha(sky.zenith, 0.28F));
        if (sky.showSun && d0 > -0.12) {
            double d2 = 128.0;
            double d3 = cam.x + Math.cos(d1) * d2;
            double d4 = cam.y + 18.0 + d0 * 96.0;
            double d5 = cam.z + Math.sin(d1) * d2 * 0.42;
            float f1 = 0.35F + 0.45F * (float)Math.max(0.0, d0);
            Mesh.orb(ctx, d3, d4, d5, 28.0F, Mesh.alpha(sky.sun, f1 * 0.28F));
            Mesh.orb(ctx, d3, d4, d5, 14.0F, Mesh.alpha(sky.sun, f1 * 0.55F));
            Mesh.orb(ctx, d3, d4, d5, 6.2F, Mesh.alpha(-2336, f1));
        }

        if (sky.showMoon && d0 < 0.35) {
            double d6 = d1 + Math.PI;
            double d7 = 118.0;
            double d8 = cam.x + Math.cos(d6) * d7;
            double d9 = cam.y + 22.0 - d0 * 88.0;
            double d10 = cam.z + Math.sin(d6) * d7 * 0.4;
            float f2 = 0.4F + 0.4F * (float)Math.max(0.0, -d0);
            Mesh.orb(ctx, d8, d9, d10, 16.0F, Mesh.alpha(sky.moon, f2 * 0.22F));
            Mesh.orb(ctx, d8, d9, d10, 5.4F, Mesh.alpha(-722689, f2 * 0.9F));
        }
    }

    private void drawClouds(WorldRenderContext ctx, Vec3d cam, AtmosphereFeature.Sky sky) {
        float f = (float)System.currentTimeMillis() * 1.8E-5F;
        int i = Math.max(8, Math.round(14.0F * VisualQuality.particleMul()));

        for (int j = 0; j < i; j++) {
            float f1 = hash(j, 17);
            float f2 = hash(j, 91);
            double d0 = f1 * Math.PI * 2.0 + f * (0.35 + j % 5 * 0.04);
            double d1 = 72.0F + f2 * 58.0F;
            double d2 = cam.x + Math.cos(d0) * d1;
            double d3 = cam.z + Math.sin(d0) * d1;
            double d4 = cam.y + 54.0 + hash(j, 4) * 28.0F + Math.sin(f * 4.0F + j) * 1.8;
            float f3 = 0.14F + 0.1F * hash(j, 33);
            float f4 = 7.5F + j % 4 * 2.2F;
            int k = Mesh.alpha(sky.cloud, f3);
            Mesh.softBillboard(ctx, d2, d4, d3, f4, k);
            Mesh.softBillboard(ctx, d2 + f4 * 0.55, d4 + 0.6, d3 + f4 * 0.2, f4 * 0.72F, Mesh.alpha(sky.cloud, f3 * 0.85F));
            Mesh.softBillboard(ctx, d2 - f4 * 0.48, d4 + 0.35, d3 - f4 * 0.18, f4 * 0.62F, Mesh.alpha(sky.cloud, f3 * 0.75F));
        }
    }

    private void drawAurora(WorldRenderContext ctx, Vec3d cam, AtmosphereFeature.Sky sky) {
        float f = (float)System.currentTimeMillis() * 5.5E-4F;
        int i = Math.max(3, Math.round(4.0F * VisualQuality.particleMul()));

        for (int j = 0; j < i; j++) {
            double d0 = j * 0.55 + f * 0.15;
            double d1 = 86 + j * 10;

            for (int k = 0; k < 18; k++) {
                double d2 = d0 + k * 0.11;
                double d3 = cam.x + Math.cos(d2) * d1;
                double d4 = cam.z + Math.sin(d2) * d1;
                double d5 = Math.sin(f * 2.2 + k * 0.35 + j) * 10.0;
                double d6 = cam.y + 62.0 + d5;
                float f1 = 0.08F + 0.16F * (0.5F + 0.5F * (float)Math.sin(f * 1.8 + k * 0.4 + j));
                int l = k % 2 == 0 ? sky.auroraA : sky.auroraB;
                Mesh.softBillboard(ctx, d3, d6, d4, 4.8F + j % 2, Mesh.alpha(l, f1));
                Mesh.softBillboard(ctx, d3, d6 + 7.5, d4, 3.4F, Mesh.alpha(l, f1 * 0.65F));
            }
        }
    }

    private void drawRain(WorldRenderContext ctx, World level, Vec3d cam, int tint) {
        if (level != null && level.isRaining()) {
            int i = Math.max(28, Math.round(52.0F * VisualQuality.particleMul()));
            float f = (float)System.currentTimeMillis() * 0.00115F;
            int j = Theme.tintMul(-4664065, tint, 0.12F);

            for (int k = 0; k < i; k++) {
                float f1 = hash(k, 3) - 0.5F;
                float f2 = hash(k, 11) - 0.5F;
                float f3 = (hash(k, 29) + f * (0.55F + hash(k, 7) * 0.45F)) % 1.0F;
                double d0 = cam.x + f1 * 22.0F;
                double d1 = cam.z + f2 * 22.0F;
                double d2 = cam.y + 8.5 - f3 * 14.5;
                float f4 = 0.028F + hash(k, 41) * 0.018F;
                Mesh.capsule(ctx, d0, d2, d1, f4, 0.22F + hash(k, 19) * 0.12F, 8, 3, Mesh.alpha(j, 0.55F), Types.fill());
                Mesh.orb(ctx, d0, d2 + 0.08, d1, f4 * 1.35F, Mesh.alpha(-1510145, 0.35F));
            }
        }
    }

    private static float dayPhase() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.world == null) {
            return 0.35F;
        } else {
            long i = TimeWeatherFeature.timeLocked() ? TimeWeatherFeature.lockedTime() : minecraftclient.world.getTimeOfDay();
            return (float)((i % 24000L + 24000L) % 24000L) / 24000.0F;
        }
    }

    private static float hash(int i, int salt) {
        int ix = i * 374761393 + salt * 668265263;
        ix = (ix ^ ix >> 13) * 1274126177;
        return (ix & 2147483647) / 2.1474836E9F;
    }

    private record Sky(int zenith, int horizon, int sun, int moon, int cloud, int auroraA, int auroraB, boolean showSun, boolean showMoon) {
    }
}
