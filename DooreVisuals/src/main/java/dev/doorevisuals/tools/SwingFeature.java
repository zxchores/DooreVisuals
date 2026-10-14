package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import net.minecraft.util.math.MathHelper;

public final class SwingFeature extends Feature {
    private static volatile boolean active;
    private static volatile String style = "\u0420\u0435\u0436\u0438\u043c 1";
    private static volatile float power = 1.0F;
    private static volatile float strength = 20.0F;
    private final Opt.Pick mode = this.opt(
        new Opt.Pick(
            "style",
            "\u0421\u0442\u0438\u043b\u044c",
            "\u0420\u0435\u0436\u0438\u043c 1",
            "\u0420\u0435\u0436\u0438\u043c 1",
            "\u0420\u0435\u0436\u0438\u043c 2",
            "\u0420\u0435\u0436\u0438\u043c 3",
            "\u0420\u0435\u0436\u0438\u043c 4",
            "\u0420\u0435\u0436\u0438\u043c 5",
            "\u041a\u043b\u0430\u0441\u0441\u0438\u043a\u0430",
            "\u041f\u043b\u0430\u0432\u043d\u044b\u0439",
            "\u0420\u0435\u0437\u043a\u0438\u0439",
            "\u041a\u0440\u0443\u0433\u043e\u0432\u043e\u0439",
            "\u0421\u0442\u0438\u043b\u0435\u0442",
            "\u0422\u044f\u0436\u0451\u043b\u044b\u0439",
            "\u0425\u0443\u043a",
            "\u0421\u0442\u0430\u0442\u0438\u0447\u043d\u044b\u0439"
        )
    );
    private final Opt.Num strengthOpt = this.opt(new Opt.Num("power", "\u0421\u0438\u043b\u0430", 20.0, 20.0, 75.0, 1.0));

    public SwingFeature() {
        super(
            "swing",
            "\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u0443\u0434\u0430\u0440\u0430",
            "\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u0438 \u0443\u0434\u0430\u0440\u0430 \u2014 5 \u0440\u0435\u0436\u0438\u043c\u043e\u0432 \u0438 \u0441\u0432\u043e\u0438 \u0441\u0442\u0438\u043b\u0438",
            Category.TOOLS,
            true
        );
    }

    public static boolean active() {
        return active;
    }

    public static String style() {
        return style;
    }

    public static float power() {
        return power;
    }

    public static float strength() {
        return strength;
    }

    public static boolean staticSwing() {
        return active && "\u0421\u0442\u0430\u0442\u0438\u0447\u043d\u044b\u0439".equals(style);
    }

    public static boolean isPlusStyle() {
        return style != null && style.startsWith("\u0420\u0435\u0436\u0438\u043c");
    }

    public static boolean suppressVanilla() {
        return active;
    }

    @Override
    protected void enable() {
        active = true;
        this.pull();
    }

    @Override
    protected void disable() {
        active = false;
    }

    @Override
    public void poke() {
        super.poke();
        if (this.on()) {
            this.pull();
        }
    }

    private void pull() {
        style = (String)this.mode.get();
        strength = MathHelper.clamp(this.strengthOpt.f(), 20.0F, 75.0F);
        power = MathHelper.clamp(0.4F + (strength - 20.0F) / 55.0F * 1.8F, 0.4F, 2.2F);
    }
}
