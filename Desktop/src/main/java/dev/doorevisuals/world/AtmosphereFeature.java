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
    private static final String VANILLA = "Ваниль";
    private static final String VANILLA_PLUS = "Ваниль+";
    private static final String CUSTOM = "Свой";
    private static final String NIGHT = "Ночь";
    private static final String SUNSET = "Закат";
    private static final String SAKURA = "Сакура";
    private static final String COLD = "Холод";
    private static final String CYBER = "Кибер";
    private final Opt.Pick preset = this.opt(
        new Opt.Pick("preset", "Пресет", VANILLA_PLUS, VANILLA_PLUS, NIGHT, SUNSET, SAKURA, COLD, CYBER, CUSTOM)
    );
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme_color", "Цвет из темы", false));
    private final Opt.Tint day = this.opt(new Opt.Tint("day", "День", -12533600));
    private final Opt.Tint dawn = this.opt(new Opt.Tint("dawn", "Рассвет", -29607));
    private final Opt.Tint dusk = this.opt(new Opt.Tint("dusk", "Закат", -4236839));
    private final Opt.Tint fogColor = this.opt(new Opt.Tint("fog_color", "Цвет тумана", -12533600));
    private final Opt.Num strength = this.opt(new Opt.Num("strength", "Сила цвета", 0.45, 0.05, 1.0, 0.05));
    private final Opt.Flag blendTime = this.opt(new Opt.Flag("time_blend", "Рассвет / закат", true));
    private final Opt.Pick timePreset = this.opt(
        new Opt.Pick("time_preset", "Время", VANILLA, VANILLA, "Рассвет", "Полдень", SUNSET, "Полночь", CUSTOM)
    );
    private final Opt.Num timeValue = this.opt(
        new Opt.Num("time", "Тики дня", 6000.0, 0.0, 24000.0, 100.0).visibleWhen(() -> CUSTOM.equals(this.timePreset.get()))
    );
    private final Opt.Num timeSpeed = this.opt(new Opt.Num("time_speed", "Скорость времени", 1.0, 0.0, 10.0, 0.25));
    private final Opt.Pick weather = this.opt(
        new Opt.Pick("weather", "Погода", VANILLA, VANILLA, "Ясно", "Дождь", "Снег", "Гроза", "Туман")
    );
    private final Opt.Num fogDensity = this.opt(new Opt.Num("fog_density", "Туман", 1.05, 0.3, 2.5, 0.05));
    private final Opt.Num fogStart = this.opt(new Opt.Num("fog_start", "Старт тумана", 0.22, 0.04, 0.8, 0.02));
    private final Opt.Num fogEnd = this.opt(new Opt.Num("fog_end", "Дальность тумана", 0.78, 0.25, 1.5, 0.05));
    private final Opt.Flag clouds = this.opt(new Opt.Flag("clouds", "Облака", true));
    private final Opt.Flag aurora = this.opt(new Opt.Flag("aurora", "Аврора", false));
    private final Opt.Flag stars = this.opt(new Opt.Flag("stars", "Звёзды", true));
    private final Opt.Flag weatherFx = this.opt(new Opt.Flag("rain_fx", "Погода FX", true));
    private final Opt.Flag vanillaSky = this.opt(new Opt.Flag("vanilla_sky", "Ванильное небо", false));
    private String lastPreset = "";
    private AtmosphereFeature.Sky painted;

    public AtmosphereFeature() {
        super("atmosphere", "Atmosphere", "Небо, туман, время и погода", Category.WORLD, false);
        this.lastPreset = (String)this.preset.get();
    }

    public static boolean hideVanillaSky() {
        return App.features()
            .find(AtmosphereFeature.class)
            .map(f -> f.on() && overworld() && !(Boolean)f.vanillaSky.get())
            .orElse(false);
    }

    public static boolean hideVanillaClouds() {
        return App.features()
            .find(AtmosphereFeature.class)
            .map(f -> f.on() && overworld() && (Boolean)f.clouds.get() && !(Boolean)f.vanillaSky.get())
            .orElse(false);
    }

    public static boolean hideVanillaRain() {
        return App.features()
            .find(AtmosphereFeature.class)
            .map(f -> f.on() && overworld() && (Boolean)f.weatherFx.get() && f.replacesPrecip())
            .orElse(false);
    }

    private static boolean overworld() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        return minecraftclient.world != null && minecraftclient.world.getRegistryKey() == World.OVERWORLD;
    }

    @Override
    protected void enable() {
        this.push();
    }

    @Override
    protected void disable() {
        AmbienceFeature.applyLive(false, 0.0F, false, 0, 0, 0, VANILLA, VANILLA);
        FogFeature.applyLive(false, 1.0F, 0.22F, 1.0F, 0.0F, 0);
        TimeWeatherFeature.applyLive(false, false, 6000, 1.0F, "default");
        this.painted = null;
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

            this.push();
            TimeWeatherFeature.advance();
        }
    }

    public void drawSky(WorldRenderContext ctx, float tickDelta) {
        if (this.on() && overworld() && !(Boolean)this.vanillaSky.get()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            ClientPlayerEntity clientplayerentity = minecraftclient.player;
            if (clientplayerentity != null && minecraftclient.world != null) {
                Vec3d vec3d = clientplayerentity.getCameraPosVec(tickDelta);
                AtmosphereFeature.Sky atmospherefeature$sky = this.liveSky();
                this.drawDome(ctx, vec3d, atmospherefeature$sky);
                this.drawCelestial(ctx, vec3d, atmospherefeature$sky);
                if ((Boolean)this.stars.get() && atmospherefeature$sky.showMoon) {
                    this.drawStars(ctx, vec3d, atmospherefeature$sky);
                }
            }
        }
    }

    public void drawFx(WorldRenderContext ctx, float tickDelta) {
        if (this.on() && overworld()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            ClientPlayerEntity clientplayerentity = minecraftclient.player;
            if (clientplayerentity != null && minecraftclient.world != null) {
                Vec3d vec3d = clientplayerentity.getCameraPosVec(tickDelta);
                AtmosphereFeature.Sky atmospherefeature$sky = this.liveSky();
                if (!(Boolean)this.vanillaSky.get() && (Boolean)this.clouds.get()) {
                    this.drawClouds(ctx, vec3d, atmospherefeature$sky);
                }

                if ((Boolean)this.aurora.get() && VisualQuality.level() != VisualQuality.Level.LOW) {
                    this.drawAurora(ctx, vec3d, atmospherefeature$sky);
                }

                if ((Boolean)this.weatherFx.get()) {
                    this.drawWeather(ctx, minecraftclient.world, vec3d, atmospherefeature$sky);
                }
            }
        }
    }

    private void push() {
        if (!this.on()) {
            this.disable();
        } else {
            int i = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.day.get();
            float f = this.fogDensity.f();
            if ("Туман".equals(this.weather.get())) {
                f = Math.max(f, 1.45F);
            }

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
            FogFeature.applyLive(true, f, this.fogStart.f(), this.fogEnd.f(), 0.62F, (Integer)this.fogColor.get());
            TimeWeatherFeature.applyLive(true, this.timeLocked(), this.lockedTime(), this.timeSpeed.f(), this.mixinWeather());
        }
    }

    private boolean timeLocked() {
        return !VANILLA.equals(this.timePreset.get());
    }

    private int lockedTime() {
        String s = (String)this.timePreset.get();
        return switch (s) {
            case "Рассвет" -> 1000;
            case "Полдень" -> 6000;
            case "Закат" -> 12500;
            case "Полночь" -> 18000;
            case "Свой" -> this.timeValue.i();
            default -> 6000;
        };
    }

    private String visualWeather() {
        String s = (String)this.weather.get();
        return switch (s) {
            case "Ясно" -> "Ясно";
            case "Дождь", "Гроза" -> "Дождь";
            case "Туман" -> "Туман";
            default -> VANILLA;
        };
    }

    private String mixinWeather() {
        String s = (String)this.weather.get();
        return switch (s) {
            case "Ясно" -> "clear";
            case "Дождь" -> "rain";
            case "Гроза" -> "thunder";
            case "Снег" -> "snow";
            case "Туман" -> "fog";
            default -> "default";
        };
    }

    private boolean replacesPrecip() {
        String s = this.mixinWeather();
        if ("rain".equals(s) || "thunder".equals(s) || "snow".equals(s)) {
            return true;
        } else {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            return minecraftclient.world != null && minecraftclient.world.isRaining();
        }
    }

    private void applyPreset(String label) {
        switch (label) {
            case "Ночь":
                this.day.set(-15062443);
                this.dawn.set(-12957056);
                this.dusk.set(-10863984);
                this.fogColor.set(-15062443);
                this.strength.set(0.72);
                this.timePreset.set("Полночь");
                this.weather.set(VANILLA);
                this.fogDensity.set(1.35);
                this.fogStart.set(0.18);
                this.fogEnd.set(0.58);
                this.aurora.set(true);
                this.stars.set(true);
                this.clouds.set(true);
                break;
            case "Закат":
                this.day.set(-30134);
                this.dawn.set(-16264);
                this.dusk.set(-4236839);
                this.fogColor.set(-38342);
                this.strength.set(0.62);
                this.timePreset.set(SUNSET);
                this.weather.set(VANILLA);
                this.fogDensity.set(1.15);
                this.fogStart.set(0.2);
                this.fogEnd.set(0.7);
                this.aurora.set(false);
                this.stars.set(false);
                this.clouds.set(true);
                break;
            case "Сакура":
                this.day.set(-18491);
                this.dawn.set(-10528);
                this.dusk.set(-1533228);
                this.fogColor.set(-18491);
                this.strength.set(0.48);
                this.timePreset.set("Рассвет");
                this.weather.set("Ясно");
                this.fogDensity.set(0.95);
                this.fogStart.set(0.28);
                this.fogEnd.set(0.85);
                this.aurora.set(false);
                this.stars.set(false);
                this.clouds.set(true);
                break;
            case "Холод":
                this.day.set(-4659969);
                this.dawn.set(-1510145);
                this.dusk.set(-8740664);
                this.fogColor.set(-4659969);
                this.strength.set(0.55);
                this.timePreset.set("Полдень");
                this.weather.set("Ясно");
                this.fogDensity.set(1.25);
                this.fogStart.set(0.16);
                this.fogEnd.set(0.62);
                this.aurora.set(true);
                this.stars.set(true);
                this.clouds.set(true);
                break;
            case "Кибер":
                this.day.set(-14622528);
                this.dawn.set(-8458284);
                this.dusk.set(-50476);
                this.fogColor.set(-14622528);
                this.strength.set(0.7);
                this.timePreset.set("Полночь");
                this.weather.set("Ясно");
                this.fogDensity.set(1.4);
                this.fogStart.set(0.14);
                this.fogEnd.set(0.55);
                this.aurora.set(true);
                this.stars.set(true);
                this.clouds.set(true);
                break;
            case "Свой":
                break;
            default:
                this.day.set(-12533600);
                this.dawn.set(-29607);
                this.dusk.set(-4236839);
                this.fogColor.set(-12533600);
                this.strength.set(0.28);
                this.timePreset.set(VANILLA);
                this.weather.set(VANILLA);
                this.fogDensity.set(0.9);
                this.fogStart.set(0.28);
                this.fogEnd.set(0.92);
                this.aurora.set(false);
                this.stars.set(false);
                this.clouds.set(true);
        }
    }

    private AtmosphereFeature.Sky liveSky() {
        int i = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.day.get();
        AtmosphereFeature.Sky atmospherefeature$sky = this.skyOf((String)this.preset.get(), i);
        if (this.painted == null) {
            this.painted = atmospherefeature$sky;
        } else {
            this.painted = this.painted.lerp(atmospherefeature$sky, 0.14F);
        }

        return this.painted;
    }

    private AtmosphereFeature.Sky skyOf(String label, int tint) {
        return switch (label) {
            case "Ночь" -> new AtmosphereFeature.Sky(
                -16315620, -15195064, -14869218, -5944, -1511169, Theme.tintMul(-7692072, tint, 0.2F), -12779622, -8758017, false, true
            );
            case "Закат" -> new AtmosphereFeature.Sky(
                -15068104, -38342, -11403384, -16264, -7992, Theme.tintMul(-12112, tint, 0.35F), -30134, -4236839, true, false
            );
            case "Сакура" -> new AtmosphereFeature.Sky(
                -6368001, -15660, -4924197, -3368, -5904, Theme.tintMul(-7958, tint, 0.4F), -18491, -1533228, true, false
            );
            case "Холод" -> new AtmosphereFeature.Sky(
                -9520897, -1510145, -8740664, -2336, -2560769, Theme.tintMul(-853249, tint, 0.25F), -8462081, -4659969, true, true
            );
            case "Кибер" -> new AtmosphereFeature.Sky(
                -16576488, -16238544, -16772848, -14622528, -50476, Theme.tintMul(-15054264, tint, 0.35F), -14622528, -50476, false, true
            );
            default -> new AtmosphereFeature.Sky(
                -11884328, -3610369, -15069152, -3896, -1510145, Theme.tintMul(-722177, tint, 0.22F), -12533600, -4236839, true, false
            );
        };
    }

    private void drawDome(WorldRenderContext ctx, Vec3d cam, AtmosphereFeature.Sky sky) {
        int i = VisualQuality.segs(20);
        Mesh.skyDome(ctx, cam.x, cam.y, cam.z, 108.0, i, 8, Mesh.alpha(sky.zenith, 0.92F), Mesh.alpha(sky.horizon, 0.88F), Mesh.alpha(sky.nadir, 0.9F), Types.sky());
    }

    private void drawCelestial(WorldRenderContext ctx, Vec3d cam, AtmosphereFeature.Sky sky) {
        float f = dayPhase();
        double d0 = Math.cos((f - 0.25F) * Math.PI * 2.0);
        double d1 = f * Math.PI * 2.0;
        if (sky.showSun && d0 > -0.12) {
            double d2 = 96.0;
            double d3 = cam.x + Math.cos(d1) * d2;
            double d4 = cam.y + d0 * 86.0;
            double d5 = cam.z + Math.sin(d1) * d2 * 0.42;
            float f1 = 0.4F + 0.5F * (float)Math.max(0.0, d0);
            Mesh.orb(ctx, d3, d4, d5, 22.0F, Mesh.alpha(sky.sun, f1 * 0.22F));
            Mesh.orb(ctx, d3, d4, d5, 11.0F, Mesh.alpha(sky.sun, f1 * 0.55F));
            Mesh.orb(ctx, d3, d4, d5, 5.4F, Mesh.alpha(-2336, f1));
        }

        if (sky.showMoon && d0 < 0.35) {
            double d6 = d1 + Math.PI;
            double d7 = cam.x + Math.cos(d6) * 90.0;
            double d8 = cam.y - d0 * 80.0;
            double d9 = cam.z + Math.sin(d6) * 90.0 * 0.4;
            float f2 = 0.45F + 0.4F * (float)Math.max(0.0, -d0);
            Mesh.orb(ctx, d7, d8, d9, 12.0F, Mesh.alpha(sky.moon, f2 * 0.2F));
            Mesh.orb(ctx, d7, d8, d9, 4.8F, Mesh.alpha(-722689, f2 * 0.9F));
        }
    }

    private void drawStars(WorldRenderContext ctx, Vec3d cam, AtmosphereFeature.Sky sky) {
        float f = 0.35F + 0.45F * (float)Math.max(0.0, -Math.cos((dayPhase() - 0.25F) * Math.PI * 2.0));
        int i = Math.max(18, Math.round(36.0F * VisualQuality.particleMul()));

        for (int j = 0; j < i; j++) {
            float f1 = hash(j, 3);
            float f2 = hash(j, 17);
            double d0 = f1 * Math.PI * 2.0;
            double d1 = 0.18 + f2 * 1.15;
            double d2 = 92.0;
            double d3 = cam.x + Math.cos(d0) * Math.sin(d1) * d2;
            double d4 = cam.y + Math.cos(d1) * d2;
            double d5 = cam.z + Math.sin(d0) * Math.sin(d1) * d2;
            float f3 = 0.35F + 0.65F * hash(j, 41);
            Mesh.orb(ctx, d3, d4, d5, 0.55F + hash(j, 7) * 0.45F, Mesh.alpha(-1, f * f3 * 0.7F));
        }
    }

    private void drawClouds(WorldRenderContext ctx, Vec3d cam, AtmosphereFeature.Sky sky) {
        float f = (float)System.currentTimeMillis() * 1.2E-5F;
        int i = Math.max(6, Math.round(10.0F * VisualQuality.particleMul()));

        for (int j = 0; j < i; j++) {
            float f1 = hash(j, 21);
            float f2 = hash(j, 73);
            double d0 = (f1 * 2.0 - 1.0) * 70.0 + Math.sin(f * 2.2 + j) * 6.0;
            double d1 = (f2 * 2.0 - 1.0) * 70.0 + Math.cos(f * 1.6 + j) * 5.0;
            double d2 = cam.x + d0;
            double d3 = cam.z + d1;
            double d4 = cam.y + 38.0 + hash(j, 9) * 10.0;
            float f3 = 0.1F + 0.08F * hash(j, 33);
            float f4 = 9.0F + j % 3 * 2.4F;
            Mesh.softBillboard(ctx, d2, d4, d3, f4, Mesh.alpha(sky.cloud, f3));
            Mesh.softBillboard(ctx, d2 + f4 * 0.5, d4 + 0.45, d3 + f4 * 0.18, f4 * 0.7F, Mesh.alpha(sky.cloud, f3 * 0.8F));
        }
    }

    private void drawAurora(WorldRenderContext ctx, Vec3d cam, AtmosphereFeature.Sky sky) {
        float f = (float)System.currentTimeMillis() * 5.5E-4F;
        int i = Math.max(2, Math.round(3.0F * VisualQuality.particleMul()));

        for (int j = 0; j < i; j++) {
            double d0 = j * 0.7 + f * 0.12;
            double d1 = 78 + j * 12;

            for (int k = 0; k < 12; k++) {
                if (VisualQuality.takeParticle()) {
                    double d2 = d0 + k * 0.14;
                    double d3 = cam.x + Math.cos(d2) * d1;
                    double d4 = cam.z + Math.sin(d2) * d1;
                    double d5 = cam.y + 58.0 + Math.sin(f * 2.0 + k * 0.4 + j) * 8.0;
                    float f1 = 0.07F + 0.14F * (0.5F + 0.5F * (float)Math.sin(f * 1.6 + k * 0.35 + j));
                    int l = k % 2 == 0 ? sky.auroraA : sky.auroraB;
                    Mesh.softBillboard(ctx, d3, d5, d4, 5.2F, Mesh.alpha(l, f1));
                }
            }
        }
    }

    private void drawWeather(WorldRenderContext ctx, World level, Vec3d cam, AtmosphereFeature.Sky sky) {
        String s = this.mixinWeather();
        boolean flag = "snow".equals(s);
        boolean flag1 = "thunder".equals(s) || level.isThundering();
        boolean flag2 = "rain".equals(s) || "thunder".equals(s) || level.isRaining();
        if (flag) {
            this.drawSnow(ctx, cam, sky);
        } else if (flag2) {
            this.drawRain(ctx, cam, sky);
        }

        if (flag1) {
            float f = (float)((System.currentTimeMillis() / 90L) % 17L);
            if (f < 2.0F) {
                Mesh.orb(ctx, cam.x, cam.y + 40.0, cam.z, 70.0F, Mesh.alpha(-1, 0.08F * (2.0F - f)));
            }
        }
    }

    private void drawRain(WorldRenderContext ctx, Vec3d cam, AtmosphereFeature.Sky sky) {
        int i = Math.max(22, Math.round(40.0F * VisualQuality.particleMul()));
        float f = (float)System.currentTimeMillis() * 0.00115F;
        int j = Theme.tintMul(-4664065, sky.horizon, 0.12F);

        for (int k = 0; k < i; k++) {
            if (VisualQuality.takeParticle()) {
                float f1 = hash(k, 3) - 0.5F;
                float f2 = hash(k, 11) - 0.5F;
                float f3 = (hash(k, 29) + f * (0.55F + hash(k, 7) * 0.45F)) % 1.0F;
                double d0 = cam.x + f1 * 20.0F;
                double d1 = cam.z + f2 * 20.0F;
                double d2 = cam.y + 8.0 - f3 * 14.0;
                float f4 = 0.026F + hash(k, 41) * 0.016F;
                Mesh.capsule(ctx, d0, d2, d1, f4, 0.2F + hash(k, 19) * 0.1F, 8, 3, Mesh.alpha(j, 0.5F), Types.fill());
            }
        }
    }

    private void drawSnow(WorldRenderContext ctx, Vec3d cam, AtmosphereFeature.Sky sky) {
        int i = Math.max(20, Math.round(34.0F * VisualQuality.particleMul()));
        float f = (float)System.currentTimeMillis() * 0.00055F;

        for (int j = 0; j < i; j++) {
            if (VisualQuality.takeParticle()) {
                float f1 = hash(j, 5) - 0.5F;
                float f2 = hash(j, 13) - 0.5F;
                float f3 = (hash(j, 31) + f * (0.25F + hash(j, 8) * 0.2F)) % 1.0F;
                double d0 = cam.x + f1 * 18.0F + Math.sin(f * 4.0F + j) * 0.8;
                double d1 = cam.z + f2 * 18.0F + Math.cos(f * 3.2F + j) * 0.7;
                double d2 = cam.y + 7.5 - f3 * 12.0;
                Mesh.orb(ctx, d0, d2, d1, 0.08F + hash(j, 19) * 0.06F, Mesh.alpha(-1, 0.55F));
            }
        }
    }

    private static float dayPhase() {
        return (float)((TimeWeatherFeature.visualTime() % 24000L + 24000L) % 24000L) / 24000.0F;
    }

    private static float hash(int i, int salt) {
        int j = i * 374761393 + salt * 668265263;
        j = (j ^ j >> 13) * 1274126177;
        return (j & 2147483647) / 2.1474836E9F;
    }

    private record Sky(int zenith, int horizon, int nadir, int sun, int moon, int cloud, int auroraA, int auroraB, boolean showSun, boolean showMoon) {
        AtmosphereFeature.Sky lerp(AtmosphereFeature.Sky to, float t) {
            return new AtmosphereFeature.Sky(
                Theme.lerp(this.zenith, to.zenith, t),
                Theme.lerp(this.horizon, to.horizon, t),
                Theme.lerp(this.nadir, to.nadir, t),
                Theme.lerp(this.sun, to.sun, t),
                Theme.lerp(this.moon, to.moon, t),
                Theme.lerp(this.cloud, to.cloud, t),
                Theme.lerp(this.auroraA, to.auroraA, t),
                Theme.lerp(this.auroraB, to.auroraB, t),
                to.showSun,
                to.showMoon
            );
        }
    }
}
