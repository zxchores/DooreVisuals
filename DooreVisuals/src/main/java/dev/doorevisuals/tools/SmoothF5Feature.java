package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

public final class SmoothF5Feature extends Feature implements Tick {
    private static volatile boolean live;
    private static volatile float blend = 1.0F;
    private static volatile boolean toDetached;
    private static volatile float durationMs = 400.0F;
    private static Perspective last = Perspective.FIRST_PERSON;
    private static long started;
    private final Opt.Num ms = this.opt(
        new Opt.Num("ms", "\u0414\u043b\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c", 400.0, 200.0, 700.0, 10.0)
    );

    public SmoothF5Feature() {
        super(
            "smooth_f5",
            "Smooth F5",
            "\u041a\u0430\u043c\u0435\u0440\u0430 \u043f\u043b\u0430\u0432\u043d\u043e \u043e\u0442\u044a\u0435\u0437\u0436\u0430\u0435\u0442 \u043f\u0440\u0438 \u0441\u043c\u0435\u043d\u0435 \u0432\u0438\u0434\u0430",
            Category.TOOLS,
            true
        );
    }

    public static boolean active() {
        return live;
    }

    public static float blend() {
        refreshBlend();
        return blend;
    }

    public static boolean transitioning() {
        if (!live) {
            return false;
        } else {
            refreshBlend();
            float f = Math.max(80.0F, durationMs);
            return (float)(System.currentTimeMillis() - started) < f;
        }
    }

    public static boolean wantDetached() {
        return toDetached;
    }

    private static void refreshBlend() {
        if (!live) {
            blend = 1.0F;
        } else {
            float f = Math.max(80.0F, durationMs);
            float f1 = (float)(System.currentTimeMillis() - started) / f;
            float f2 = MathHelper.clamp(f1, 0.0F, 1.0F);
            f2 = f2 * f2 * f2 * (f2 * (f2 * 6.0F - 15.0F) + 10.0F);
            blend = toDetached ? f2 : 1.0F - f2;
        }
    }

    @Override
    protected void enable() {
        live = true;
    }

    @Override
    protected void disable() {
        live = false;
        blend = 1.0F;
    }

    @Override
    public void tick(MinecraftClient mc) {
        live = this.on();
        durationMs = this.ms.f();
        if (this.on() && mc.options != null) {
            Perspective perspective = mc.options.getPerspective();
            if (perspective != last) {
                toDetached = !perspective.isFirstPerson();
                started = System.currentTimeMillis();
                last = perspective;
            }
        } else {
            blend = 1.0F;
        }
    }
}
