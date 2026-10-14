package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Theme;

public final class InventoryTintFeature extends Feature {
    private static volatile boolean live;
    private static volatile int tint = Theme.ACCENT;
    private static volatile float strength = 0.18F;
    private final Opt.Flag enabled = this.opt(
        new Opt.Flag("tint", "\u041e\u0442\u0442\u0435\u043d\u043e\u043a \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044f", true)
    );
    private final Opt.Num opacity = this.opt(new Opt.Num("opacity", "\u0421\u0438\u043b\u0430", 0.18, 0.05, 0.45, 0.02));
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme_color", "\u0426\u0432\u0435\u0442 \u0438\u0437 \u0442\u0435\u043c\u044b", true));

    public InventoryTintFeature() {
        super(
            "inventory_tint",
            "Inventory Tint",
            "\u0422\u0451\u043c\u043d\u044b\u0439 \u043e\u0442\u0442\u0435\u043d\u043e\u043a \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440\u043e\u0432 \u0432\u043c\u0435\u0441\u0442\u043e \u0431\u0435\u043b\u043e\u0439 \u043f\u0430\u043d\u0435\u043b\u0438",
            Category.OVERLAY,
            true
        );
    }

    public static boolean active() {
        return live && strength > 0.01F;
    }

    public static int scrimArgb() {
        float f = Math.min(0.28F, strength);
        int i = -16118510;
        if (ThemeFeature.glassActive()) {
            f = Math.min(0.34F, f + 0.06F * ThemeFeature.glassStrength());
            i = Theme.lerp(i, ThemeFeature.glassTint(), 0.12F);
        } else if (tint != 0) {
            i = Theme.lerp(i, tint, 0.1F);
        }

        return Theme.alpha(i, Math.round(255.0F * f));
    }

    @Override
    protected void enable() {
        live = (Boolean)this.enabled.get();
        this.sync();
    }

    @Override
    protected void disable() {
        live = false;
    }

    @Override
    public void poke() {
        super.poke();
        live = this.on() && (Boolean)this.enabled.get();
        this.sync();
    }

    private void sync() {
        strength = this.opacity.f();
        tint = this.themeColor.get() ? ThemeFeature.hudTint() : Theme.ACCENT;
    }
}
