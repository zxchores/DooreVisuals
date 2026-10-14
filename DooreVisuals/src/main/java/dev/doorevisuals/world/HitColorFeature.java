package dev.doorevisuals.world;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Theme;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

public final class HitColorFeature extends Feature {
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442", -50614));
    private final Opt.Flag useTheme = this.opt(new Opt.Flag("theme", "\u0426\u0432\u0435\u0442 \u0442\u0435\u043c\u044b", false));
    private final Opt.Num strength = this.opt(new Opt.Num("strength", "\u0421\u0438\u043b\u0430", 0.65, 0.15, 1.0, 0.05));

    public HitColorFeature() {
        super(
            "hit_color",
            "\u0426\u0432\u0435\u0442 \u0443\u0434\u0430\u0440\u0430",
            "\u0417\u0430\u043b\u0438\u0432\u0430\u0435\u0442 \u043c\u043e\u0434\u0435\u043b\u044c\u043a\u0443 \u0446\u0432\u0435\u0442\u043e\u043c \u043f\u0440\u0438 \u043f\u043e\u043b\u0443\u0447\u0435\u043d\u0438\u0438 \u0443\u0440\u043e\u043d\u0430",
            Category.WORLD,
            false
        );
    }

    public static boolean active() {
        return App.features() != null && App.features().find(HitColorFeature.class).map(Feature::on).orElse(false);
    }

    public static int modelTint() {
        return App.features().find(HitColorFeature.class).filter(Feature::on).map(HitColorFeature::computeTint).orElse(-1);
    }

    private int computeTint() {
        int i = this.useTheme.get() ? Theme.EFFECT : (Integer)this.color.get();
        float f = MathHelper.clamp(this.strength.f(), 0.0F, 1.0F);
        int j = lerpChannel(255, i >> 16 & 0xFF, f);
        int k = lerpChannel(255, i >> 8 & 0xFF, f);
        int l = lerpChannel(255, i & 0xFF, f);
        return ColorHelper.getArgb(255, j, k, l);
    }

    private static int lerpChannel(int from, int to, float t) {
        return MathHelper.clamp(Math.round(from + (to - from) * t), 0, 255);
    }
}
