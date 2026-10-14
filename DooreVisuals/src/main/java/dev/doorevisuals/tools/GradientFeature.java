package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Theme;

/** Global animated gradient palette used by UI gradients when enabled. */
public final class GradientFeature extends Feature {
    private static volatile boolean live;
    private static volatile int colorA = 0xFFC9B8FF;
    private static volatile int colorB = 0xFFE7DFFF;
    private static volatile float speed = 0.16F;
    private static volatile boolean animated = true;

    private final Opt.Tint a = this.opt(new Opt.Tint("color_a", "Цвет 1", colorA));
    private final Opt.Tint b = this.opt(new Opt.Tint("color_b", "Цвет 2", colorB));
    private final Opt.Num speedOpt = this.opt(new Opt.Num("speed", "Скорость", 0.16, 0.0, 1.0, 0.01));
    private final Opt.Flag animatedOpt = this.opt(new Opt.Flag("animated", "Анимация", true));

    public GradientFeature() {
        super("gradient_system", "Gradient System", "Единый настраиваемый градиент для интерфейса", Category.SYSTEM, false);
        this.sync();
    }

    public static boolean active() { return live; }

    public static int apply(int original, float position) {
        if (!live) return original;
        int alpha = original >>> 24;
        float phase = animated ? Anim.timeSec() * speed * 0.22F : 0.0F;
        float t = 0.5F + 0.5F * (float)Math.sin((position + phase) * Math.PI);
        int rgb = Theme.lerp(colorA | 0xFF000000, colorB | 0xFF000000, t);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    @Override
    public void poke() {
        super.poke();
        this.sync();
    }

    @Override
    protected void enable() { live = true; this.sync(); }
    @Override
    protected void disable() { live = false; }

    private void sync() {
        colorA = a.get();
        colorB = b.get();
        speed = speedOpt.f();
        animated = animatedOpt.get();
    }
}
