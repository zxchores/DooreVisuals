package dev.doorevisuals.world;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.VisualQuality;
import dev.doorevisuals.tools.ThemeFeature;
import dev.doorevisuals.tools.TimeWeatherFeature;
import dev.doorevisuals.world.sky.AuroraRenderer;
import dev.doorevisuals.world.sky.CloudRenderer;
import dev.doorevisuals.world.sky.SkyPalette;
import dev.doorevisuals.world.sky.SkyPreset;
import dev.doorevisuals.world.sky.SkyRenderer;
import dev.doorevisuals.world.sky.WeatherRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Options and wiring for the sky, fog, time and weather. The drawing itself lives in
 * {@link dev.doorevisuals.world.sky}, and the look of each preset in
 * {@code assets/doorevisuals/atmosphere/presets.json}.
 */
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
    private final Opt.Num auroraStrength = this.opt(
        new Opt.Num("aurora_strength", "Сила авроры", 0.6, 0.1, 1.0, 0.05).visibleWhen(() -> (Boolean)this.aurora.get())
    );
    private final Opt.Flag ownCelestial = this.opt(new Opt.Flag("own_celestial", "Свои светила", false));
    private final Opt.Flag weatherFx = this.opt(new Opt.Flag("rain_fx", "Вспышки грозы", true));
    private final Opt.Num precipTint = this.opt(new Opt.Num("precip_tint", "Тон осадков", 0.0, 0.0, 1.0, 0.05));
    private final Opt.Flag vanillaSky = this.opt(new Opt.Flag("vanilla_sky", "Ванильное небо", false));
    private final SkyRenderer skyRenderer = new SkyRenderer();
    private final CloudRenderer cloudRenderer = new CloudRenderer();
    private final AuroraRenderer auroraRenderer = new AuroraRenderer();
    private String lastPreset = "";
    private SkyPalette painted;
    private float paintedAt;

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

    public static boolean hideVanillaCelestial() {
        return App.features()
            .find(AtmosphereFeature.class)
            .map(f -> f.on() && overworld() && !(Boolean)f.vanillaSky.get() && (Boolean)f.ownCelestial.get())
            .orElse(false);
    }

    /** Colour vanilla precipitation is pulled toward, or zero when it should be left alone. */
    public static int precipTint() {
        return App.features()
            .find(AtmosphereFeature.class)
            .filter(f -> f.on() && overworld() && f.precipTint.f() > 0.002F)
            .map(f -> f.livePalette().horizon() & 16777215 | Math.round(f.precipTint.f() * 255.0F) << 24)
            .orElse(0);
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
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (this.on() && overworld() && !(Boolean)this.vanillaSky.get() && minecraftclient.world != null) {
            this.skyRenderer.draw(ctx, this.livePalette(), SkyRenderer.radius(minecraftclient), dayPhase(), (Boolean)this.ownCelestial.get());
        }
    }

    public void drawFx(WorldRenderContext ctx, float tickDelta) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (this.on() && overworld() && minecraftclient.world != null) {
            SkyPalette skypalette = this.livePalette();
            Vec3d vec3d = minecraftclient.gameRenderer.getCamera().getCameraPos();
            if (!(Boolean)this.vanillaSky.get() && (Boolean)this.clouds.get()) {
                this.cloudRenderer.draw(ctx, skypalette, vec3d, SkyRenderer.radius(minecraftclient) * 0.5);
            }

            if ((Boolean)this.aurora.get() && VisualQuality.level() != VisualQuality.Level.LOW) {
                this.auroraRenderer.draw(ctx, skypalette, this.auroraStrength.f() * nightWeight());
            }

            if ((Boolean)this.weatherFx.get() && ("thunder".equals(this.mixinWeather()) || minecraftclient.world.isThundering())) {
                WeatherRenderer.lightning(ctx, vec3d);
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

    private void applyPreset(String label) {
        if (SkyPreset.has(label)) {
            SkyPreset skypreset = SkyPreset.of(label);
            this.day.set(skypreset.day());
            this.dawn.set(skypreset.dawn());
            this.dusk.set(skypreset.dusk());
            this.fogColor.set(skypreset.fogColor());
            this.strength.set((double)skypreset.strength());
            this.timePreset.set(skypreset.time());
            this.weather.set(skypreset.weather());
            this.fogDensity.set((double)skypreset.fogDensity());
            this.fogStart.set((double)skypreset.fogStart());
            this.fogEnd.set((double)skypreset.fogEnd());
            this.aurora.set(skypreset.aurora());
            this.clouds.set(skypreset.clouds());
        }
    }

    private SkyPalette livePalette() {
        int i = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.day.get();
        SkyPalette skypalette = SkyPalette.of(SkyPreset.of((String)this.preset.get()), i);
        float f = Anim.timeSec();
        if (this.painted == null) {
            this.painted = skypalette;
        } else {
            float f1 = Math.min(0.25F, Math.max(0.0F, f - this.paintedAt));
            this.painted = this.painted.lerp(skypalette, 1.0F - (float)Math.exp(-9.0F * f1));
        }

        this.paintedAt = f;
        return this.painted;
    }

    private static float dayPhase() {
        return (float)((TimeWeatherFeature.visualTime() % 24000L + 24000L) % 24000L) / 24000.0F;
    }

    /** Aurora only reads as aurora against a dark sky, so it follows the sun below the horizon. */
    private static float nightWeight() {
        return (float)Math.max(0.0, Math.min(1.0, -Math.cos((dayPhase() - 0.25F) * Math.PI * 2.0) * 1.6 + 0.35));
    }
}
