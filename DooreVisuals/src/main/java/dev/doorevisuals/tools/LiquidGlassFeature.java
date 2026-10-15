package dev.doorevisuals.tools;

import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Theme;

/**
 * Liquid Glass: switches the Liquid Glass look on everywhere the client paints a glass panel or a HUD plate: the ClickGUI,
 * the cards, the HUD widgets and the other windows. Off, everything returns to the style of the chosen theme.
 */
public final class LiquidGlassFeature extends Feature {
    private final Opt.Num clear = this.opt(new Opt.Num("clear", "Прозрачность стекла", 0.6, 0.3, 1.0, 0.05));

    public LiquidGlassFeature() {
        super(
                "liquid_glass",
                "Liquid Glass",
                "Тема жидкого стекла для всех панелей и виджетов",
                Category.OVERLAY,
                false
        );
        this.badge(Badge.NEW);
    }

    @Override
    protected void enable() {
        Theme.LIQUID_A = this.clear.f();
        Theme.LIQUID = true;
    }

    @Override
    protected void disable() {
        Theme.LIQUID = false;
    }

    @Override
    public void poke() {
        super.poke();
        if (this.on()) {
            Theme.LIQUID_A = this.clear.f();
        }
    }
}
