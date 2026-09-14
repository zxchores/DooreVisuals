package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class CrosshairFeature extends Feature {
    private final Opt.Num size = this.opt(new Opt.Num("size", "\u0420\u0430\u0434\u0438\u0443\u0441", 6.5, 3.0, 14.0, 0.5));
    private final Opt.Num thick = this.opt(new Opt.Num("thick", "\u0422\u043e\u043b\u0449\u0438\u043d\u0430", 1.35, 0.6, 3.0, 0.1));
    private final Opt.Flag glow = this.opt(new Opt.Flag("glow", "\u0421\u0432\u0435\u0447\u0435\u043d\u0438\u0435", true));

    public CrosshairFeature() {
        super(
            "crosshair",
            "Crosshair",
            "\u0421\u0432\u0435\u0442\u044f\u0449\u0435\u0435\u0441\u044f \u043a\u043e\u043b\u044c\u0446\u043e \u043f\u043e\u0434 \u0446\u0432\u0435\u0442 \u0442\u0435\u043c\u044b",
            Category.OVERLAY,
            true
        );
    }

    public void paint(DrawContext g) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (!minecraftclient.options.hudHidden) {
            float f = g.getScaledWindowWidth() * 0.5F;
            float f1 = g.getScaledWindowHeight() * 0.5F;
            float f2 = this.size.f();
            float f3 = this.thick.f();
            int i = Theme.HUD;
            if (Nvg.frame()) {
                Ui.push();
                if ((Boolean)this.glow.get()) {
                    Nvg.circle(f, f1, f2 + f3 * 3.4F, Theme.alpha(i, 28));
                    Nvg.circle(f, f1, f2 + f3 * 1.8F, Theme.alpha(i, 48));
                }

                Nvg.ring(f - f2, f1 - f2, f2 * 2.0F, f2 * 2.0F, f3 + 0.55F, Theme.alpha(i, 70), f2);
                Nvg.ring(f - f2, f1 - f2, f2 * 2.0F, f2 * 2.0F, f3, i, f2);
                Ui.pop();
            } else {
                Paint.outline(g, f - f2, f1 - f2, f2 * 2.0F, f2 * 2.0F, i, f2);
            }
        }
    }

    public static boolean hideVanilla() {
        return App.features().find(CrosshairFeature.class).filter(Feature::on).isPresent();
    }
}
