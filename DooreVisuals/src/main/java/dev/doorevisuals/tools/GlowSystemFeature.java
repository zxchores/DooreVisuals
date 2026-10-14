package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Theme;

/** Shared accent-glow profile used by UI shadows and emissive world effects. */
public final class GlowSystemFeature extends Feature {
    private static volatile boolean live;
    private static volatile int rgb = 0xC9B8FF;
    private static volatile int alpha = 48;
    private static volatile float radius = 10.0F;

    private final Opt.Tint color = this.opt(new Opt.Tint("color", "Цвет glow", rgb));
    private final Opt.Num strength = this.opt(new Opt.Num("strength", "Сила", 48.0, 10.0, 100.0, 2.0));
    private final Opt.Num radiusOpt = this.opt(new Opt.Num("radius", "Радиус", 10.0, 4.0, 22.0, 1.0));

    public GlowSystemFeature() {
        super("glow_system", "Glow System", "Единая система подсветки для UI и world effects", Category.SYSTEM, true);
        this.sync();
    }

    public static boolean active() { return live; }
    public static int color() { return rgb; }
    public static int alpha() { return alpha; }
    public static float radius() { return radius; }

    public static int accentColor(int base) {
        return Theme.lerp(base | 0xFF000000, rgb | 0xFF000000, 0.72F);
    }

    @Override
    public void poke() { super.poke(); this.sync(); }
    @Override
    protected void enable() { live = true; this.sync(); }
    @Override
    protected void disable() { live = false; }

    private void sync() {
        rgb = color.get() & 0xFFFFFF;
        alpha = Math.round(strength.f());
        radius = radiusOpt.f();
    }
}
