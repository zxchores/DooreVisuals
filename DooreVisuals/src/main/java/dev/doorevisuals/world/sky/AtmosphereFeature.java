package dev.doorevisuals.world.sky;

import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.world.AmbienceFeature;
import dev.doorevisuals.world.FogFeature;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.VisualQuality;
import dev.doorevisuals.tools.ThemeFeature;
import dev.doorevisuals.tools.TimeWeatherFeature;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
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
    private static final String AUTO = "Авто";
    private final Opt.Pick preset = this.opt(
        new Opt.Pick("preset", "Пресет", VANILLA_PLUS, AUTO, VANILLA_PLUS, NIGHT, SUNSET, SAKURA, COLD, CYBER, "Новый год", CUSTOM)
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
    private final Opt.Pick moonSkin = this.opt(new Opt.Pick("moon_skin", "Поверхность луны", "Кратеры", "Нет", "Кратеры", "Файл").chips());
    private final Opt.Text moonFilePath = this.opt(new Opt.Text("moon_file", "Картинка луны (путь к PNG, из буфера)", "").visibleWhen(() -> "Файл".equals(this.moonSkin.get())));
    private final Opt.Flag ownCelestial = this.opt(new Opt.Flag("own_celestial", "Свои светила", true));
    private final Opt.Pick meteors = this.opt(new Opt.Pick("meteors", "Падающие звёзды", "Редко", "Нет", "Редко", "Часто"));
    private final Opt.Flag weatherFx = this.opt(new Opt.Flag("rain_fx", "Вспышки грозы", true));
    private final Opt.Num precipTint = this.opt(new Opt.Num("precip_tint", "Тон осадков", 0.0, 0.0, 1.0, 0.05));
    private final Opt.Flag vanillaSky = this.opt(new Opt.Flag("vanilla_sky", "Ванильное небо", false));
    private final Opt.Tint zenithColor = this.opt(new Opt.Tint("sky_zenith", "Зенит", -12146984).visibleWhen(() -> CUSTOM.equals(this.preset.get())));
    private final Opt.Tint horizonColor = this.opt(new Opt.Tint("sky_horizon", "Горизонт", -3602177).visibleWhen(() -> CUSTOM.equals(this.preset.get())));
    private final Opt.Tint nadirColor = this.opt(new Opt.Tint("sky_nadir", "Низ неба", -15069152).visibleWhen(() -> CUSTOM.equals(this.preset.get())));
    private final Opt.Tint sunColor = this.opt(new Opt.Tint("sky_sun", "Солнце", -1048).visibleWhen(() -> CUSTOM.equals(this.preset.get())));
    private final Opt.Tint moonColor = this.opt(new Opt.Tint("sky_moon", "Луна", -1510145).visibleWhen(() -> CUSTOM.equals(this.preset.get())));
    private final Opt.Tint cloudColor = this.opt(new Opt.Tint("sky_cloud", "Облака", -722177).visibleWhen(() -> CUSTOM.equals(this.preset.get())));
    private final Opt.Tint auroraColorA = this.opt(new Opt.Tint("aurora_a", "Аврора · цвет 1", -12533600).visibleWhen(() -> CUSTOM.equals(this.preset.get())));
    private final Opt.Tint auroraColorB = this.opt(new Opt.Tint("aurora_b", "Аврора · цвет 2", -4236839).visibleWhen(() -> CUSTOM.equals(this.preset.get())));
    private final Opt.Num turbidity = this.opt(new Opt.Num("sky_turbidity", "Рассеивание света", 0.42, 0.05, 1.2, 0.02).visibleWhen(() -> CUSTOM.equals(this.preset.get())));
    private final Opt.Num coverage = this.opt(new Opt.Num("cloud_coverage", "Плотность облаков", 0.5, 0.0, 1.0, 0.02).visibleWhen(() -> CUSTOM.equals(this.preset.get())));
    private final Opt.Num cloudSpeed = this.opt(new Opt.Num("cloud_speed", "Скорость облаков", 1.0, 0.05, 3.0, 0.05).visibleWhen(() -> CUSTOM.equals(this.preset.get())));
    private final Opt.Num windLow = this.opt(new Opt.Num("wind_low", "Ветер · нижний слой", 0.55, 0.1, 2.5, 0.05).visibleWhen(this.clouds::get));
    private final Opt.Num windMid = this.opt(new Opt.Num("wind_mid", "Ветер · средний слой", 0.9, 0.1, 2.5, 0.05).visibleWhen(this.clouds::get));
    private final Opt.Num windHigh = this.opt(new Opt.Num("wind_high", "Ветер · верхний слой", 1.35, 0.1, 2.5, 0.05).visibleWhen(this.clouds::get));
    private final Opt.Flag drifts = this.opt(new Opt.Flag("snow_drifts", "Сугробы", true));
    private final Opt.Num snowSpeed = this.opt(new Opt.Num("snow_speed", "Скорость накопления снега", 1.0, 0.2, 4.0, 0.1).visibleWhen(() -> (Boolean)this.drifts.get()));
    private final Opt.Pick snowIntensity = this.opt(new Opt.Pick("snow_intensity", "Снегопад", "Обычный", "Лёгкий", "Обычный", "Сильный", "Метель"));
    private final Opt.Flag rainDetails = this.opt(new Opt.Flag("rain_details", "Капли и лужи", true));
    private final Opt.Flag footprints = this.opt(new Opt.Flag("footprints", "Следы шагов", true));
    private final Opt.Num auroraSpeed = this.opt(new Opt.Num("aurora_speed", "Скорость авроры", 1.0, 0.1, 2.5, 0.05).visibleWhen(() -> CUSTOM.equals(this.preset.get())));
    private final Opt.Tint precipitationColor = this.opt(
        new Opt.Tint("precipitation_color", "Цвет осадков", -3602177).visibleWhen(() -> this.precipTint.f() > 0.002F)
    );
    private final SkyRenderer skyRenderer = new SkyRenderer();
    private final CloudRenderer cloudRenderer = new CloudRenderer();
    private final AuroraRenderer auroraRenderer = new AuroraRenderer();
    private final AtmosphereEffectsRenderer surfaceEffects = new AtmosphereEffectsRenderer();
    private String lastPreset = "";
    private SkyPalette painted;
    private float paintedAt;

    public AtmosphereFeature() {
        super("atmosphere", "Atmosphere", "Небо, туман, время и погода", Category.WORLD, false);
        this.lastPreset = (String)this.preset.get();
        this.applyPaletteOptions(SkyPreset.of(this.lastPreset));
    }

    public static boolean hideVanillaSky() {
        return App.features()
            .find(AtmosphereFeature.class)
            .map(f -> f.on() && inWorld() && !(Boolean)f.vanillaSky.get())
            .orElse(false);
    }

    public static boolean hideVanillaClouds() {
        return App.features()
            .find(AtmosphereFeature.class)
            .map(f -> f.on() && inWorld() && (Boolean)f.clouds.get() && !(Boolean)f.vanillaSky.get())
            .orElse(false);
    }

    public static boolean hideVanillaCelestial() {
        return App.features()
            .find(AtmosphereFeature.class)
            .map(f -> f.on() && inWorld() && !(Boolean)f.vanillaSky.get() && (Boolean)f.ownCelestial.get())
            .orElse(false);
    }

    /** Colour vanilla precipitation is pulled toward, or zero when it should be left alone. */
    public static int precipTint() {
        return App.features()
            .find(AtmosphereFeature.class)
            .filter(f -> f.on() && overworld() && f.precipTint.f() > 0.002F)
            .map(f -> (f.precipitationColor.get() & 16777215) | Math.round(f.precipTint.f() * 255.0F) << 24)
            .orElse(0);
    }

    private static boolean overworld() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        return minecraftclient.world != null && minecraftclient.world.getRegistryKey() == World.OVERWORLD;
    }

    private static boolean inWorld() { return MinecraftClient.getInstance().world != null; }

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
        this.surfaceEffects.clear();
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
            String selectedWeather = (String)this.weather.get();
            boolean visualRain = this.visualRain(mc);
            boolean visualStorm = "Гроза".equals(selectedWeather) || (VANILLA.equals(selectedWeather) && mc.world != null && mc.world.isThundering());
            this.surfaceEffects.tick(mc, visualRain, visualStorm, this.footprints.get(), this.visualSnow(mc), this.snowSpeed.f(), this.snowIntensityValue());
        } else {
            this.surfaceEffects.clear();
        }
    }

    private long nextSunCheckAt;
    private float sunVisibility = 1.0F;

    /** Whether it is precipitating where the player is, and what kind: the chosen weather, or the real one in vanilla mode. */
    private boolean realPrecipitation(MinecraftClient mc) {
        return VANILLA.equals(this.weather.get()) && mc.world != null && mc.player != null && mc.world.isRaining();
    }

    private boolean coldHere(MinecraftClient mc) {
        BlockPos pos = mc.player.getBlockPos();
        return mc.world.getBiome(pos).value().getPrecipitation(pos, mc.world.getSeaLevel()) == Biome.Precipitation.SNOW;
    }

    private boolean visualRain(MinecraftClient mc) {
        if (!(Boolean)this.rainDetails.get()) {
            return false;
        }
        String selected = (String)this.weather.get();
        return "Дождь".equals(selected) || "Гроза".equals(selected) || (this.realPrecipitation(mc) && !this.coldHere(mc));
    }

    private boolean visualSnow(MinecraftClient mc) {
        return "Снег".equals(this.weather.get()) || (this.realPrecipitation(mc) && this.coldHere(mc));
    }

    private float snowIntensityValue() {
        return switch ((String)this.snowIntensity.get()) {
            case "Лёгкий" -> 0.5F;
            case "Сильный" -> 1.8F;
            case "Метель" -> 3.0F;
            default -> 1.0F;
        };
    }

    /** How bright the snow on the ground is: full by day, dim at night, since it is drawn flat without block light. */
    private float snowLight() {
        float up = (float)Math.cos((dayPhase() - 0.25F) * Math.PI * 2.0);
        return Math.max(0.28F, Math.min(1.0F, up * 0.9F + 0.62F));
    }

    private float meteorRate() {
        return switch ((String)this.meteors.get()) {
            case "Нет" -> 0.0F;
            case "Часто" -> 0.85F;
            default -> 0.35F;
        };
    }

    public void drawSky(WorldRenderContext ctx, float tickDelta) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (this.on() && inWorld() && !(Boolean)this.vanillaSky.get()) {
            this.skyRenderer.draw(
                ctx,
                this.livePalette(),
                SkyRenderer.radius(minecraftclient),
                dayPhase(),
                (Boolean)this.ownCelestial.get(),
                moonPhase(minecraftclient),
                this.sunVisibility(minecraftclient),
                this.meteorRate()
            );

            // Туман берёт цвет из той же палитры, что и небо: пресет и время суток ведут его за собой.
            FogFeature.setTint(this.livePalette().fog());
        }
    }

    /**
     * Доля лунного цикла: 0 — полная луна, 0.5 — новолуние (ванильный счёт фаз).
     */
    /** The surface of the moon: "Нет" (the shader's own), "Кратеры" or "Файл". */
    public static String moonSkin() {
        return dev.doorevisuals.App.features().find(AtmosphereFeature.class).map(f -> f.moonSkin.get()).orElse("Нет");
    }

    public static String moonFile() {
        return dev.doorevisuals.App.features().find(AtmosphereFeature.class).map(f -> f.moonFilePath.get()).orElse("");
    }

    private static float moonPhase(MinecraftClient mc) {
        World world = mc.world;
        if (world == null) {
            return 0.0F;
        }

        int phase = (int)(world.getTimeOfDay() / 24000L % 8L);
        if (phase < 0) {
            phase += 8;
        }

        return phase / 8.0F;
    }

    /**
     * Гасит солнечные лучи, когда между камерой и солнцем стоит рельеф. Луч пускается раз в полсекунды,
     * поэтому проверка незаметна по цене.
     */
    private float sunVisibility(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        if (now < this.nextSunCheckAt) {
            return this.sunVisibility;
        }

        this.nextSunCheckAt = now + 500L;
        World world = mc.world;
        if (world == null || mc.player == null) {
            return this.sunVisibility = 1.0F;
        }

        float phase = dayPhase() - (float)Math.floor(dayPhase());
        double az = phase * Math.PI * 2.0;
        double sunUp = Math.cos((phase - 0.25F) * Math.PI * 2.0);
        double radial = Math.sqrt(Math.max(0.0, 1.0 - sunUp * sunUp));
        Vec3d direction = new Vec3d(Math.cos(az) * radial, sunUp, Math.sin(az) * radial);
        Vec3d from = mc.gameRenderer.getCamera().getCameraPos();
        Vec3d to = from.add(direction.multiply(96.0));

        BlockHitResult hit = world.raycast(
            new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)
        );

        return this.sunVisibility = hit.getType() == HitResult.Type.MISS ? 1.0F : 0.0F;
    }

    public void drawFx(WorldRenderContext ctx, float tickDelta) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (this.on() && inWorld()) {
            SkyPalette skypalette = this.livePalette();
            Vec3d vec3d = minecraftclient.gameRenderer.getCamera().getCameraPos();
            if (!(Boolean)this.vanillaSky.get() && (Boolean)this.clouds.get()) {
                this.cloudRenderer.draw(ctx, skypalette, vec3d, SkyRenderer.radius(minecraftclient) * 0.82,
                    this.windLow.f(), this.windMid.f(), this.windHigh.f(), dayPhase());
            }

            if (overworld() && (Boolean)this.aurora.get() && VisualQuality.level() != VisualQuality.Level.LOW) {
                this.auroraRenderer.draw(ctx, skypalette, this.auroraStrength.f() * nightWeight());
            }

            if (overworld() && (Boolean)this.weatherFx.get() && ("Гроза".equals(this.weather.get()) || minecraftclient.world.isThundering())) {
                WeatherRenderer.lightning(ctx, vec3d);
            }
            this.surfaceEffects.draw(ctx, this.visualRain(minecraftclient), this.footprints.get(), this.visualSnow(minecraftclient), skypalette.fog(), (Boolean)this.drifts.get(), this.snowLight());
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
            this.applyPaletteOptions(skypreset);
        }
    }

    private void applyPaletteOptions(SkyPreset preset) {
        this.zenithColor.set(preset.zenith());
        this.horizonColor.set(preset.horizon());
        this.nadirColor.set(preset.nadir());
        this.sunColor.set(preset.sun());
        this.moonColor.set(preset.moon());
        this.cloudColor.set(preset.cloud());
        this.auroraColorA.set(preset.auroraA());
        this.auroraColorB.set(preset.auroraB());
        this.turbidity.set((double)preset.turbidity());
        this.coverage.set((double)preset.coverage());
        this.cloudSpeed.set(1.0);
        this.auroraSpeed.set(1.0);
        this.precipitationColor.set(preset.horizon());
    }

    private SkyPalette livePalette() {
        int i = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.day.get();
        SkyPreset preset = SkyPreset.of((String)this.preset.get());
        boolean custom = CUSTOM.equals(this.preset.get());
        SkyPalette source = AUTO.equals(this.preset.get()) ? this.automaticPalette(i) : SkyPalette.of(preset, i);
        SkyPalette skypalette = custom
            ? new SkyPalette(
                (Integer)this.zenithColor.get(),
                (Integer)this.horizonColor.get(),
                (Integer)this.nadirColor.get(),
                (Integer)this.fogColor.get(),
                (Integer)this.sunColor.get(),
                (Integer)this.moonColor.get(),
                dev.doorevisuals.draw.Theme.tintMul((Integer)this.cloudColor.get(), i, preset.cloudMix()),
                (Integer)this.auroraColorA.get(),
                (Integer)this.auroraColorB.get(),
                this.turbidity.f(),
                this.coverage.f(),
                this.cloudSpeed.f(),
                this.auroraSpeed.f()
            )
            : new SkyPalette(
                source.zenith(), source.horizon(), source.nadir(), source.fog(), source.sun(), source.moon(), source.cloud(),
                source.auroraA(), source.auroraB(), source.turbidity(), source.coverage(),
                this.cloudSpeed.f(), this.auroraSpeed.f()
            );
        float f = Anim.timeSec();
        if (this.painted == null) {
            this.painted = skypalette;
        } else {
            float f1 = Math.min(0.25F, Math.max(0.0F, f - this.paintedAt));
            this.painted = this.painted.lerp(skypalette, 1.0F - (float)Math.exp(-3.5F * f1));
        }

        this.paintedAt = f;
        SkyPalette result = VANILLA.equals(this.timePreset.get()) ? this.painted.afterDark(nightWeight()) : this.painted;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null && mc.world.getRegistryKey() == World.NETHER) {
            SkyPalette nether = new SkyPalette(0xFF321B25, 0xFF7B3429, 0xFF180E18, 0xFF3A1A1C, 0xFFFF784A, 0xFFB83D54, 0xFF6E382D,
                0xFFEF593D, 0xFFFFB34F, result.turbidity()*0.7F, result.coverage()*0.75F, result.cloudSpeed(), result.auroraSpeed());
            return CUSTOM.equals(this.preset.get()) ? result.lerp(nether, 0.28F) : nether;
        }
        if (mc.world != null && mc.world.getRegistryKey() == World.END) {
            SkyPalette end = new SkyPalette(0xFF100E26, 0xFF40345E, 0xFF090817, 0xFF161232, 0xFFD5C9FF, 0xFFCEBFFF, 0xFF51436F,
                0xFF9B6BFF, 0xFF51DCE8, result.turbidity()*0.85F, result.coverage()*0.65F, result.cloudSpeed(), result.auroraSpeed());
            return CUSTOM.equals(this.preset.get()) ? result.lerp(end, 0.28F) : end;
        }
        return result;
    }

    private SkyPalette automaticPalette(int accent) {
        MinecraftClient mc = MinecraftClient.getInstance();
        SkyPalette palette = SkyPalette.of(SkyPreset.of(VANILLA_PLUS), accent);
        if (mc.world != null && mc.player != null) {
            String biome = mc.world.getBiome(BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ()))
                .getKey().map(key -> key.getValue().getPath()).orElse("");
            SkyPalette biomePalette = null;
            if (biome.contains("desert") || biome.contains("badlands") || biome.contains("savanna")) {
                biomePalette = SkyPalette.of(SkyPreset.of(SUNSET), accent);
            } else if (biome.contains("snow") || biome.contains("ice") || biome.contains("frozen") || biome.contains("grove")) {
                biomePalette = SkyPalette.of(SkyPreset.of(COLD), accent);
            } else if (biome.contains("cherry") || biome.contains("jungle") || biome.contains("flower") || biome.contains("mushroom")) {
                biomePalette = SkyPalette.of(SkyPreset.of(SAKURA), accent);
            }
            if (biomePalette != null) palette = palette.lerp(biomePalette, 0.58F);
        }

        float phase = dayPhase();
        float dawn = Math.max(0.0F, 1.0F - Math.min(phase, 1.0F - phase) / 0.12F);
        float dusk = Math.max(0.0F, 1.0F - Math.abs(phase - 0.5F) / 0.12F);
        if (dawn > 0.0F) palette = palette.lerp(SkyPalette.of(SkyPreset.of(SAKURA), accent), dawn * 0.22F);
        if (dusk > 0.0F) palette = palette.lerp(SkyPalette.of(SkyPreset.of(SUNSET), accent), dusk * 0.5F);
        return palette;
    }

    private static float dayPhase() {
        return (float)((TimeWeatherFeature.visualTime() % 24000L + 24000L) % 24000L) / 24000.0F;
    }

    /** Aurora only reads as aurora against a dark sky, so it follows the sun below the horizon. */
    private static float nightWeight() {
        return (float)Math.max(0.0, Math.min(1.0, -Math.cos((dayPhase() - 0.25F) * Math.PI * 2.0) * 1.6 + 0.35));
    }
}
