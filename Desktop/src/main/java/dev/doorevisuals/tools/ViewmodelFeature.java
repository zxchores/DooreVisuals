package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Types;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Arm;

public final class ViewmodelFeature extends Feature implements Tick {
    private static volatile boolean active;
    private static volatile float mainX;
    private static volatile float mainY;
    private static volatile float mainZ;
    private static volatile float mainS = 1.0F;
    private static volatile float offX;
    private static volatile float offY;
    private static volatile float offZ;
    private static volatile float offS = 1.0F;
    private static volatile boolean glowLive;
    private static volatile float glowStr = 0.7F;
    private static volatile int glowRgb = -2690470;
    private final Opt.Num mx = this.opt(new Opt.Num("main_x", "\u041e\u0441\u043d\u043e\u0432\u043d\u0430\u044f X", 0.0, -1.5, 1.5, 0.05));
    private final Opt.Num my = this.opt(new Opt.Num("main_y", "\u041e\u0441\u043d\u043e\u0432\u043d\u0430\u044f Y", 0.0, -1.5, 1.5, 0.05));
    private final Opt.Num mz = this.opt(new Opt.Num("main_z", "\u041e\u0441\u043d\u043e\u0432\u043d\u0430\u044f Z", 0.0, -1.5, 1.5, 0.05));
    private final Opt.Num ms = this.opt(
        new Opt.Num("main_scale", "\u041e\u0441\u043d\u043e\u0432\u043d\u0430\u044f \u0448\u043a\u0430\u043b\u0430", 1.0, 0.4, 2.0, 0.05)
    );
    private final Opt.Num ox = this.opt(new Opt.Num("off_x", "\u0412\u0442\u043e\u0440\u0430\u044f X", 0.0, -1.5, 1.5, 0.05));
    private final Opt.Num oy = this.opt(new Opt.Num("off_y", "\u0412\u0442\u043e\u0440\u0430\u044f Y", 0.0, -1.5, 1.5, 0.05));
    private final Opt.Num oz = this.opt(new Opt.Num("off_z", "\u0412\u0442\u043e\u0440\u0430\u044f Z", 0.0, -1.5, 1.5, 0.05));
    private final Opt.Num os = this.opt(new Opt.Num("off_scale", "\u0412\u0442\u043e\u0440\u0430\u044f \u0448\u043a\u0430\u043b\u0430", 1.0, 0.4, 2.0, 0.05));
    private final Opt.Flag handGlow = this.opt(new Opt.Flag("hand_glow", "\u0421\u0432\u0435\u0447\u0435\u043d\u0438\u0435 \u0440\u0443\u043a\u0438", true));
    private final Opt.Tint glowColor = this.opt(
        new Opt.Tint("glow_color", "\u0426\u0432\u0435\u0442 \u0441\u0432\u0435\u0447\u0435\u043d\u0438\u044f", -2690470).visibleWhen(this.handGlow::get)
    );
    private final Opt.Num glowPower = this.opt(
        new Opt.Num("glow_power", "\u0421\u0438\u043b\u0430 \u0441\u0432\u0435\u0447\u0435\u043d\u0438\u044f", 0.7, 0.15, 1.4, 0.05)
            .visibleWhen(this.handGlow::get)
    );

    public ViewmodelFeature() {
        super(
            "viewmodel",
            "Viewmodel",
            "\u041f\u043e\u0437\u0438\u0446\u0438\u044f \u0440\u0443\u043a \u0438 \u0441\u0432\u0435\u0447\u0435\u043d\u0438\u0435 first-person",
            Category.TOOLS,
            true
        );
    }

    public static boolean active() {
        return active;
    }

    public static float mainX() {
        return mainX;
    }

    public static float mainY() {
        return mainY;
    }

    public static float mainZ() {
        return mainZ;
    }

    public static float mainS() {
        return mainS;
    }

    public static float offX() {
        return offX;
    }

    public static float offY() {
        return offY;
    }

    public static float offZ() {
        return offZ;
    }

    public static float offS() {
        return offS;
    }

    public static boolean glow() {
        return glowLive;
    }

    public static void submitGlow(MatrixStack pose, OrderedRenderCommandQueue collector, Arm arm) {
        if (glowLive && pose != null && collector != null) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.options.getPerspective() == Perspective.FIRST_PERSON) {
                float f = glowStr;
                int i = glowRgb;
                float f1 = 0.18F + 0.42F * f;
                float[] afloat = new float[]{(i >> 16 & 0xFF) / 255.0F, (i >> 8 & 0xFF) / 255.0F, (i & 0xFF) / 255.0F, f1};
                pose.push();
                float f2 = 1.05F + 0.1F * f;
                pose.scale(f2, f2, f2);
                boolean flag = arm == Arm.LEFT;
                collector.submitCustom(pose, Types.glow(), (p, buf) -> drawArmGlow(p, buf, afloat, flag, f));
                pose.pop();
            }
        }
    }

    private static void drawArmGlow(Entry p, VertexConsumer buf, float[] c, boolean left, float str) {
        float f = left ? -1.0F : 1.0F;
        float f1 = 0.085F + 0.03F * str;
        glowBox(p, buf, f * 0.02F - f1, -0.18F, -0.12F, f * 0.02F + f1, 0.42F, 0.14F, c);
        float[] afloat = new float[]{c[0], c[1], c[2], c[3] * 0.45F};
        glowBox(p, buf, f * 0.02F - f1 * 1.55F, -0.22F, -0.16F, f * 0.02F + f1 * 1.55F, 0.48F, 0.2F, afloat);
    }

    private static void glowBox(Entry p, VertexConsumer buf, float x0, float y0, float z0, float x1, float y1, float z1, float[] c) {
        quad(buf, p, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, c);
        quad(buf, p, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, c);
        quad(buf, p, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, c);
        quad(buf, p, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, c);
        quad(buf, p, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, c);
        quad(buf, p, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, c);
    }

    private static void quad(
        VertexConsumer buf,
        Entry p,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float x4,
        float y4,
        float z4,
        float[] c
    ) {
        buf.vertex(p, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        buf.vertex(p, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
        buf.vertex(p, x3, y3, z3).color(c[0], c[1], c[2], c[3]);
        buf.vertex(p, x4, y4, z4).color(c[0], c[1], c[2], c[3]);
    }

    @Override
    protected void enable() {
        active = true;
        this.sync();
    }

    @Override
    protected void disable() {
        active = false;
        glowLive = false;
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on()) {
            this.sync();
        }
    }

    private void sync() {
        mainX = this.mx.f();
        mainY = this.my.f();
        mainZ = this.mz.f();
        mainS = this.ms.f();
        offX = this.ox.f();
        offY = this.oy.f();
        offZ = this.oz.f();
        offS = this.os.f();
        glowLive = this.on() && (Boolean)this.handGlow.get();
        glowStr = this.glowPower.f();
        glowRgb = (Integer)this.glowColor.get();
    }
}
