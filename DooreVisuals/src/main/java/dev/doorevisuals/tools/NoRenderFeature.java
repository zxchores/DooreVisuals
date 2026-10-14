package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;

public final class NoRenderFeature extends Feature {
    private static volatile boolean live;
    private static volatile boolean water;
    private static volatile boolean fire;
    private static volatile boolean blockInFace;
    private static volatile boolean hurtCam;
    private static volatile boolean bobbing;
    private static volatile boolean pumpkin;
    private static volatile boolean portal;
    private static volatile boolean nausea;
    private static volatile boolean vignette;
    private static volatile boolean totem;
    private static volatile boolean weather;
    private static volatile boolean explosions;
    private static volatile boolean signs;
    private static volatile boolean grass;
    private final Opt.Flag optWater = this.opt(new Opt.Flag("water", "\u0412\u043e\u0434\u0430", true));
    private final Opt.Flag optFire = this.opt(new Opt.Flag("fire", "\u041e\u0433\u043e\u043d\u044c / \u043b\u0430\u0432\u0430", true));
    private final Opt.Flag optBlock = this.opt(new Opt.Flag("block", "\u0411\u043b\u043e\u043a \u0432 \u043b\u0438\u0446\u043e", true));
    private final Opt.Flag optHurt = this.opt(new Opt.Flag("hurt", "\u0422\u0440\u044f\u0441\u043a\u0430 \u0443\u0440\u043e\u043d\u0430", true));
    private final Opt.Flag optBob = this.opt(new Opt.Flag("bob", "\u041f\u043e\u043a\u0430\u0447\u0438\u0432\u0430\u043d\u0438\u0435", false));
    private final Opt.Flag optPumpkin = this.opt(new Opt.Flag("pumpkin", "\u0422\u044b\u043a\u0432\u0430", true));
    private final Opt.Flag optPortal = this.opt(new Opt.Flag("portal", "\u041f\u043e\u0440\u0442\u0430\u043b", true));
    private final Opt.Flag optNausea = this.opt(new Opt.Flag("nausea", "\u0422\u043e\u0448\u043d\u043e\u0442\u0430", true));
    private final Opt.Flag optVignette = this.opt(new Opt.Flag("vignette", "\u0412\u0438\u043d\u044c\u0435\u0442\u043a\u0430", false));
    private final Opt.Flag optTotem = this.opt(new Opt.Flag("totem", "\u0422\u043e\u0442\u0435\u043c", false));
    private final Opt.Flag optWeather = this.opt(new Opt.Flag("weather", "\u0414\u043e\u0436\u0434\u044c / \u0441\u043d\u0435\u0433", false));
    private final Opt.Flag optExplosions = this.opt(new Opt.Flag("explosions", "\u0412\u0437\u0440\u044b\u0432\u044b", false));
    private final Opt.Flag optSigns = this.opt(new Opt.Flag("signs", "\u0422\u0430\u0431\u043b\u0438\u0447\u043a\u0438", false));
    private final Opt.Flag optGrass = this.opt(new Opt.Flag("grass", "\u0422\u0440\u0430\u0432\u0430", false));

    public NoRenderFeature() {
        super(
            "no_render",
            "No Render",
            "\u0423\u0431\u0440\u0430\u0442\u044c \u043e\u0432\u0435\u0440\u043b\u0435\u0439 \u0432\u043e\u0434\u044b/\u043b\u0430\u0432\u044b, \u0442\u0440\u044f\u0441\u043a\u0443 \u044d\u043a\u0440\u0430\u043d\u0430, \u0442\u044b\u043a\u0432\u0443, \u043f\u043e\u0440\u0442\u0430\u043b, \u0442\u0440\u0430\u0432\u0443 \u0438 \u0434\u0440\u0443\u0433\u043e\u0435",
            Category.WORLD,
            false
        );
    }

    public static boolean water() {
        return live && water;
    }

    public static boolean fire() {
        return live && fire;
    }

    public static boolean blockInFace() {
        return live && blockInFace;
    }

    public static boolean hurtCam() {
        return live && hurtCam;
    }

    public static boolean bobbing() {
        return live && bobbing;
    }

    public static boolean pumpkin() {
        return live && pumpkin;
    }

    public static boolean portal() {
        return live && portal;
    }

    public static boolean nausea() {
        return live && nausea;
    }

    public static boolean vignette() {
        return live && vignette;
    }

    public static boolean totem() {
        return live && totem;
    }

    public static boolean weather() {
        return live && weather;
    }

    public static boolean explosions() {
        return live && explosions;
    }

    public static boolean signs() {
        return live && signs;
    }

    public static boolean grass() {
        return live && grass;
    }

    public static boolean hideParticle(ParticleEffect options) {
        if (options == null) {
            return false;
        } else {
            ParticleType<?> particletype = options.getType();
            return !fire()
                    || particletype != ParticleTypes.FLAME
                        && particletype != ParticleTypes.SOUL_FIRE_FLAME
                        && particletype != ParticleTypes.LAVA
                        && particletype != ParticleTypes.DRIPPING_LAVA
                        && particletype != ParticleTypes.FALLING_LAVA
                        && particletype != ParticleTypes.LANDING_LAVA
                        && particletype != ParticleTypes.SMOKE
                        && particletype != ParticleTypes.LARGE_SMOKE
                        && particletype != ParticleTypes.CAMPFIRE_COSY_SMOKE
                        && particletype != ParticleTypes.CAMPFIRE_SIGNAL_SMOKE
                ? explosions()
                    && (particletype == ParticleTypes.EXPLOSION || particletype == ParticleTypes.EXPLOSION_EMITTER || particletype == ParticleTypes.FLASH)
                : true;
        }
    }

    public static boolean isGrass(BlockState state) {
        return state.isOf(Blocks.SHORT_GRASS)
            || state.isOf(Blocks.TALL_GRASS)
            || state.isOf(Blocks.FERN)
            || state.isOf(Blocks.LARGE_FERN)
            || state.isOf(Blocks.SHORT_DRY_GRASS)
            || state.isOf(Blocks.TALL_DRY_GRASS);
    }

    @Override
    protected void enable() {
        live = true;
        this.sync();
        this.reloadChunksIfNeeded();
    }

    @Override
    protected void disable() {
        live = false;
        this.reloadChunksIfNeeded();
    }

    @Override
    public void poke() {
        super.poke();
        boolean flag = grass;
        this.sync();
        if (this.on() && flag != grass) {
            this.reloadChunksIfNeeded();
        }
    }

    private void sync() {
        water = (Boolean)this.optWater.get();
        fire = (Boolean)this.optFire.get();
        blockInFace = (Boolean)this.optBlock.get();
        hurtCam = (Boolean)this.optHurt.get();
        bobbing = (Boolean)this.optBob.get();
        pumpkin = (Boolean)this.optPumpkin.get();
        portal = (Boolean)this.optPortal.get();
        nausea = (Boolean)this.optNausea.get();
        vignette = (Boolean)this.optVignette.get();
        totem = (Boolean)this.optTotem.get();
        weather = (Boolean)this.optWeather.get();
        explosions = (Boolean)this.optExplosions.get();
        signs = (Boolean)this.optSigns.get();
        grass = (Boolean)this.optGrass.get();
    }

    private void reloadChunksIfNeeded() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.worldRenderer != null) {
            minecraftclient.worldRenderer.reload();
        }
    }
}
