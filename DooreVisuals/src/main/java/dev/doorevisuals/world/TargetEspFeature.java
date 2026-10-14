package dev.doorevisuals.world;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.tools.ThemeFeature;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3fc;

public final class TargetEspFeature extends Feature {
    public static final String STYLE_GHOST = "Ghosts";
    public static final String STYLE_BOX = "Box";
    public static final String STYLE_CIRCLE = "Circle";
    private static final int ORBS = 3;
    private static final int LINKS = 12;
    private final Opt.Pick kind = this.opt(
        new Opt.Pick(
            "style",
            "\u0412\u0438\u0434",
            STYLE_GHOST,
            STYLE_GHOST, STYLE_BOX, STYLE_CIRCLE
        )
    );
    private final Opt.Num size = this.opt(new Opt.Num("radius", "\u0420\u0430\u0437\u043c\u0435\u0440", 1.0, 0.65, 1.65, 0.05));
    private final Opt.Num speed = this.opt(new Opt.Num("speed", "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c", 0.7, 0.2, 2.2, 0.05));
    private final Opt.Num density = this.opt(
        new Opt.Num("density", "\u041f\u043b\u043e\u0442\u043d\u043e\u0441\u0442\u044c \u0447\u0430\u0441\u0442\u0438\u0446", 0.7, 0.35, 1.4, 0.05).visibleWhen(() -> STYLE_GHOST.equals(this.kind.get()))
    );
    private final Opt.Num glow = this.opt(new Opt.Num("softness", "\u0421\u0432\u0435\u0447\u0435\u043d\u0438\u0435", 1.0, 0.25, 1.6, 0.05));
    private final Opt.Num boxFill = this.opt(new Opt.Num("box_fill", "\u0417\u0430\u043b\u0438\u0432\u043a\u0430", 0.10, 0.0, 0.5, 0.02).visibleWhen(() -> STYLE_BOX.equals(this.kind.get())));
    private final Opt.Num boxThickness = this.opt(new Opt.Num("box_thickness", "\u0422\u043e\u043b\u0449\u0438\u043d\u0430", 1.6, 0.6, 4.0, 0.2).visibleWhen(() -> STYLE_BOX.equals(this.kind.get())));
    private final Opt.Flag remember = this.opt(new Opt.Flag("stick", "\u041f\u043e\u043c\u043d\u0438\u0442\u044c \u0446\u0435\u043b\u044c", true));
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme_color", "\u0426\u0432\u0435\u0442 \u0438\u0437 \u0442\u0435\u043c\u044b", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442", -1).visibleWhen(() -> !(Boolean)this.themeColor.get()));
    private final Anim appear = new Anim(0.0F);
    private final double[] ox = new double[3];
    private final double[] oy = new double[3];
    private final double[] oz = new double[3];
    private boolean followReady;
    private LivingEntity shown;
    private double phase;
    private long frameNanos;
    private float boxX;
    private float boxY;
    private float boxW;
    private float boxH;
    private float boxA;
    private boolean boxLive;

    public TargetEspFeature() {
        super(
            "target_esp",
            "\u0426\u0435\u043b\u044c ESP",
            "\u0428\u0430\u0440\u044b, \u043f\u0440\u0438\u0437\u0440\u0430\u043a\u0438, \u0446\u0435\u043f\u0438 \u0438\u043b\u0438 2D-\u0440\u0430\u043c\u043a\u0430. \u0420\u0430\u0441\u043a\u0440\u043e\u0439 \u043c\u043e\u0434\u0443\u043b\u044c \u2014 \u0432\u0438\u0434 \u0438 \u0441\u043b\u0430\u0439\u0434\u0435\u0440\u044b",
            Category.WORLD,
            true
        );
    }

    public void draw(WorldRenderContext ctx, float partial) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        float f = this.deltaSeconds();
        LivingEntity livingentity = this.select(minecraftclient);
        this.boxLive = false;
        if (livingentity != null && !visible(minecraftclient, livingentity)) {
            this.clearImmediately();
        } else {
            if (livingentity != null) {
                if (this.shown != livingentity) {
                    this.shown = livingentity;
                    this.appear.snap(0.0F);
                    this.followReady = false;
                }

                this.appear.to(1.0F, 7.5F, f);
            } else {
                this.appear.to(0.0F, 8.0F, f);
            }

            float f1 = MathHelper.clamp(Anim.easeOut(this.appear.value()), 0.0F, 1.0F);
            if (this.shown != null && !(f1 < 0.015F)) {
                if (!visible(minecraftclient, this.shown)) {
                    this.clearImmediately();
                } else {
                    this.phase = wrap(this.phase + f * this.speed.f() * 0.28);
                    Box box = interpolatedBox(this.shown, partial);
                    switch ((String)this.kind.get()) {
                        case STYLE_BOX -> {
                            int edge = Mesh.alpha(this.tint(), f1);
                            Mesh.box(ctx, box, Mesh.alpha(this.tint(), f1 * this.boxFill.f()), 0);
                            Mesh.cornerBox(ctx, box, edge, Math.min((float)(box.maxY-box.minY)*0.28F, 0.38F), this.boxThickness.f());
                            this.captureBox(minecraftclient, box, f1);
                        }
                        case STYLE_CIRCLE -> {
                            double h = box.maxY - box.minY;
                            double cycle = (Math.sin(this.phase * Math.PI * 2.0) + 1.0) * 0.5;
                            double y = box.minY + h * cycle;
                            double r = Math.max(box.maxX - box.minX, box.maxZ - box.minZ) * 0.8 * this.size.f();
                            Mesh.torus(ctx, (box.minX + box.maxX) * 0.5, y, (box.minZ + box.maxZ) * 0.5, r, 0.018 * this.glow.f(), 48, 8, Mesh.alpha(this.tint(), f1), Types.glow());
                        }
                        default -> this.renderGhosts(ctx, this.shown, box, f, partial, f1);
                    }

                    if (!this.boxLive) {
                        this.boxA *= 0.78F;
                    }
                }
            } else {
                if (f1 < 0.015F) {
                    this.shown = null;
                    this.followReady = false;
                }
            }
        }
    }

    private void renderSigil(WorldRenderContext ctx, Box b, float dt, float opacity) {
        double cx = (b.minX + b.maxX) * 0.5, cz = (b.minZ + b.maxZ) * 0.5;
        double h = b.maxY - b.minY, r = Math.max(b.maxX - b.minX, b.maxZ - b.minZ) * 0.72 * this.size.f();
        float pulse = 0.88F + 0.12F * (float)Math.sin(this.phase * 8.0);
        int base = Mesh.alpha(this.tint(), opacity * pulse);
        int halo = Mesh.alpha(Theme.lerp(this.tint(), 0xFFFFFFFF, 0.5F), opacity * 0.48F * this.glow.f());
        // The box follows the interpolated target bounds; the rune wraps its feet and torso.
        Mesh.box(ctx, b, 0, Mesh.alpha(base, 0.70F));
        Mesh.torus(ctx, cx, b.minY + 0.025, cz, r, 0.012 * this.glow.f(), 42, 8, halo, Types.glow());
        Mesh.torus(ctx, cx, b.minY + 0.035, cz, r * 0.86, 0.009, 36, 6, base, Types.holo());
        double phase = this.phase * 5.0;
        for (int arc = 0; arc < 3; arc++) {
            double a = phase + arc * Math.PI * 2 / 3;
            double y = b.minY + h * (0.35 + 0.24 * Math.sin(a * 0.6));
            double px = cx + Math.cos(a) * r, pz = cz + Math.sin(a) * r;
            Mesh.orb(ctx, px, y, pz, 0.045F * (float)this.size.f(), halo);
            Mesh.orb(ctx, px, y, pz, 0.020F, base);
        }
        for (int i = 0; i < 4; i++) {
            double a = phase * 0.42 + i * Math.PI / 2;
            double px = cx + Math.cos(a) * r * 0.78, pz = cz + Math.sin(a) * r * 0.78;
            double py = b.minY + h * (0.25 + 0.65 * ((Math.sin(a * 0.73) + 1) * 0.5));
            Mesh.softBillboard(ctx, px, py, pz, 0.07F, halo);
        }
    }

    public void paintHud(DrawContext g) {
        if (this.on() && "Box".equals(this.kind.get()) && !(this.boxA < 0.02F) && !(this.boxW < 4.0F)) {
            float f = 0.9F + 0.1F * (float)Math.sin(Anim.timeSec() * (1.6 + this.speed.f()));
            float f1 = this.boxX + this.boxW * 0.5F;
            float f2 = this.boxY + this.boxH * 0.5F;
            float f3 = this.boxW * f;
            float f4 = this.boxH * f;
            float f5 = f1 - f3 * 0.5F;
            float f6 = f2 - f4 * 0.5F;
            int i = Mesh.alpha(this.tint(), this.boxA);
            float f7 = Math.min(f3, f4) * 0.34F;
            float f8 = 2.4F + 2.2F * this.glow.f();
            drawCorners(f5, f6, f3, f4, f7, f8 * 2.4F, Mesh.alpha(i, this.boxA * 0.28F));
            drawCorners(f5, f6, f3, f4, f7, f8, i);
        }
    }

    private void renderOrbs(WorldRenderContext ctx, Box box, float dt, float opacity) {
        this.placeOrbit(box, dt, 1.0);
        float f = this.size.f();
        float f1 = this.glow.f();
        float f2 = (0.11F + 0.12F * f) * (0.75F + 0.4F * f1);
        int i = Mesh.alpha(Theme.lerp(this.tint(), -1, 0.12F), opacity);

        for (int j = 0; j < 3; j++) {
            float f3 = 0.94F + 0.06F * (float)Math.sin(this.phase * Math.PI * 2.0 + j * 2.1);
            float f4 = f2 * f3;
            Mesh.orb(ctx, this.ox[j], this.oy[j], this.oz[j], f4 * 1.45F, Mesh.alpha(i, 0.32F));
            Mesh.orb(ctx, this.ox[j], this.oy[j], this.oz[j], f4, i);
        }
    }

    private void renderGhosts(WorldRenderContext ctx, LivingEntity target, Box box, float dt, float partial, float opacity) {
        this.placeOrbit(box, dt, 1.55);
        float f = this.size.f();
        float f1 = this.glow.f();
        float f2 = (0.08F + 0.07F * f) * (0.72F + 0.4F * f1);
        int i = Theme.lerp(this.tint(), -1, 0.16F);
        int j = Math.max(6, Math.min(22, 6 + Math.round(this.density.f() * 10.0F)));

        for (int k = 0; k < 3; k++) {
            double d0 = this.phase * Math.PI * 2.0 + k * (Math.PI * 2.0 / 3.0);
            double[] lead = this.orbitPoint(box, d0, 1.55);
            double sx = this.ox[k] - lead[0];
            double sy = this.oy[k] - lead[1];
            double sz = this.oz[k] - lead[2];

            for (int l = 0; l < j; l++) {
                double d1 = d0 - l * 0.11;
                double[] adouble = this.orbitPoint(box, d1, 1.55);
                adouble[0] += sx;
                adouble[1] += sy;
                adouble[2] += sz;
                float f3 = 1.0F - (float)l / j;
                float f4 = f3 * f3;
                boolean flag = l == 0;
                float f5 = f2 * (flag ? 1.15F : 0.35F + 0.7F * f3);
                Mesh.orb(ctx, adouble[0], adouble[1], adouble[2], f5 * (flag ? 1.45F : 1.15F), Mesh.alpha(i, opacity * f4 * (flag ? 0.28F : 0.16F)));
                Mesh.orb(ctx, adouble[0], adouble[1], adouble[2], f5, Mesh.alpha(this.tint(), opacity * f4 * (flag ? 0.95F : 0.55F)));
            }
        }
    }

    private void placeOrbit(Box box, float dt, double length) {
        float f = 1.0F - (float)Math.exp(-dt * 9.5);

        for (int i = 0; i < 3; i++) {
            double d0 = this.phase * Math.PI * 2.0 + i * (Math.PI * 2.0 / 3.0);
            double[] adouble = this.orbitPoint(box, d0, length);
            if (!this.followReady) {
                this.ox[i] = adouble[0];
                this.oy[i] = adouble[1];
                this.oz[i] = adouble[2];
            } else {
                this.ox[i] = this.ox[i] + (adouble[0] - this.ox[i]) * f;
                this.oy[i] = this.oy[i] + (adouble[1] - this.oy[i]) * f;
                this.oz[i] = this.oz[i] + (adouble[2] - this.oz[i]) * f;
            }
        }

        this.followReady = true;
    }

    private double[] orbitPoint(Box box, double a, double length) {
        double d0 = (box.minX + box.maxX) * 0.5;
        double d1 = (box.minY + box.maxY) * 0.5;
        double d2 = (box.minZ + box.maxZ) * 0.5;
        double d3 = (0.42 + 0.38 * this.size.f()) * (0.55 + 0.7 * this.density.f());
        double d4 = 0.42 * length * this.size.f();
        return new double[]{d0 + Math.cos(a) * d3, d1 + Math.sin(a * 1.15) * d4, d2 + Math.sin(a) * d3};
    }

    private void renderChains(WorldRenderContext ctx, Box box, float opacity) {
        double d0 = (box.minX + box.maxX) * 0.5;
        double d1 = (box.minY + box.maxY) * 0.5;
        double d2 = (box.minZ + box.maxZ) * 0.5;
        float f = this.size.f();
        double d3 = 0.55 + 0.25 * this.density.f();
        double d4 = (0.42 + 0.38 * this.size.f()) * d3;
        double d5 = d4;
        double d6 = d4;
        double d7 = 0.95 * this.size.f();
        float f1 = 0.085F + 0.04F * f;
        float f2 = f1 * 0.52F;
        float f3 = (0.016F + 0.01F * f) * (0.7F + 0.4F * this.glow.f());
        int i = Mesh.alpha(Theme.lerp(this.tint(), -1, 0.35F), opacity * 0.95F);
        int j = Mesh.alpha(i, opacity * 0.28F);
        double d8 = 1.65;

        for (int k = 0; k < 12; k++) {
            double d9 = wrap((k + this.phase * 12.0) / 12.0);
            double d10 = wrap((k + 1 + this.phase * 12.0) / 12.0);
            Vec3d vec3d = helix(d0, d1, d2, d5, d6, d7, d9, d8);
            Vec3d vec3d1 = helix(d0, d1, d2, d5, d6, d7, d10, d8);
            double d11 = vec3d1.x - vec3d.x;
            double d12 = vec3d1.y - vec3d.y;
            double d13 = vec3d1.z - vec3d.z;
            double d14 = d9 * Math.PI * 2.0 * d8;
            double d15 = Math.cos(d14);
            double d16 = Math.sin(d14);
            double d17;
            double d18;
            double d19;
            if ((k & 1) == 0) {
                d17 = d15;
                d18 = 0.22;
                d19 = d16;
            } else {
                d17 = d12 * d16;
                d18 = d13 * d15 - d11 * d16;
                d19 = -d12 * d15;
            }

            Mesh.chainLink(ctx, vec3d.x, vec3d.y, vec3d.z, d11, d12, d13, d17, d18, d19, f1, f2, f3 * 1.35F, j);
            Mesh.chainLink(ctx, vec3d.x, vec3d.y, vec3d.z, d11, d12, d13, d17, d18, d19, f1, f2, f3, i);
        }
    }

    private static Vec3d helix(double cx, double cy, double cz, double rx, double rz, double h, double u, double turns) {
        double d0 = u * Math.PI * 2.0 * turns;
        return new Vec3d(cx + Math.cos(d0) * rx, cy + (u - 0.5) * h, cz + Math.sin(d0) * rz);
    }

    private void captureBox(MinecraftClient mc, Box box, float opacity) {
        double d0 = box.maxY - box.minY;
        double d1 = box.minY + d0 * 0.52;
        double d2 = d0 * 0.22;
        double d3 = Math.max(box.maxX - box.minX, box.maxZ - box.minZ) * 0.55 * this.size.f() + 0.02;
        float f = Float.POSITIVE_INFINITY;
        float f1 = Float.POSITIVE_INFINITY;
        float f2 = Float.NEGATIVE_INFINITY;
        float f3 = Float.NEGATIVE_INFINITY;
        int i = 0;

        for (int j = 0; j < 8; j++) {
            double d4 = ((j & 1) == 0 ? -d3 : d3) + (box.minX + box.maxX) * 0.5;
            double d5 = (j & 2) == 0 ? d1 - d2 : d1 + d2;
            double d6 = ((j & 4) == 0 ? -d3 : d3) + (box.minZ + box.maxZ) * 0.5;
            float[] afloat = project(mc, d4, d5, d6);
            if (afloat != null) {
                f = Math.min(f, afloat[0]);
                f1 = Math.min(f1, afloat[1]);
                f2 = Math.max(f2, afloat[0]);
                f3 = Math.max(f3, afloat[1]);
                i++;
            }
        }

        if (i < 4) {
            this.boxA *= 0.7F;
        } else {
            float f5 = f2 - f;
            float f6 = f3 - f1;
            if (!(f5 < 8.0F) && !(f6 < 8.0F) && !(f5 > mc.getWindow().getScaledWidth() * 0.7F)) {
                if (!this.boxLive && this.boxA < 0.05F) {
                    this.boxX = f;
                    this.boxY = f1;
                    this.boxW = f5;
                    this.boxH = f6;
                } else {
                    float f4 = 0.28F;
                    this.boxX = this.boxX + (f - this.boxX) * f4;
                    this.boxY = this.boxY + (f1 - this.boxY) * f4;
                    this.boxW = this.boxW + (f5 - this.boxW) * f4;
                    this.boxH = this.boxH + (f6 - this.boxH) * f4;
                }

                this.boxA = opacity;
                this.boxLive = true;
            }
        }
    }

    private static float[] project(MinecraftClient mc, double x, double y, double z) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d vec3d = new Vec3d(x, y, z).subtract(camera.getCameraPos());
        Vector3fc vector3fc = camera.getHorizontalPlane();
        if (vec3d.x * vector3fc.x() + vec3d.y * vector3fc.y() + vec3d.z * vector3fc.z() < 0.12) {
            return null;
        } else {
            Vec3d vec3d1 = mc.gameRenderer.project(new Vec3d(x, y, z));
            if (!(vec3d1.z < -1.0) && !(vec3d1.z > 1.0)) {
                float f = (float)((vec3d1.x + 1.0) * 0.5 * mc.getWindow().getScaledWidth());
                float f1 = (float)((1.0 - vec3d1.y) * 0.5 * mc.getWindow().getScaledHeight());
                return new float[]{f, f1};
            } else {
                return null;
            }
        }
    }

    private static void drawCorners(float x, float y, float w, float h, float len, float thick, int argb) {
        float f = thick * 0.5F;
        Nvg.rect(x, y, len, thick, argb, f);
        Nvg.rect(x, y, thick, len, argb, f);
        Nvg.circle(x + f, y + f, f * 1.15F, argb);
        Nvg.rect(x + w - len, y, len, thick, argb, f);
        Nvg.rect(x + w - thick, y, thick, len, argb, f);
        Nvg.circle(x + w - f, y + f, f * 1.15F, argb);
        Nvg.rect(x, y + h - thick, len, thick, argb, f);
        Nvg.rect(x, y + h - len, thick, len, argb, f);
        Nvg.circle(x + f, y + h - f, f * 1.15F, argb);
        Nvg.rect(x + w - len, y + h - thick, len, thick, argb, f);
        Nvg.rect(x + w - thick, y + h - len, thick, len, argb, f);
        Nvg.circle(x + w - f, y + h - f, f * 1.15F, argb);
    }

    private static Box interpolatedBox(LivingEntity target, float partial) {
        double d0 = MathHelper.lerp(partial, target.lastX, target.getX());
        double d1 = MathHelper.lerp(partial, target.lastY, target.getY());
        double d2 = MathHelper.lerp(partial, target.lastZ, target.getZ());
        return target.getBoundingBox().offset(d0 - target.getX(), d1 - target.getY(), d2 - target.getZ());
    }

    private LivingEntity select(MinecraftClient mc) {
        if (mc.world != null && mc.player != null) {
            LivingEntity livingentity = App.aim().get();
            if (livingentity == null
                || livingentity == mc.player
                || !livingentity.isAlive()
                || livingentity.getEntityWorld() != mc.world
                || !(Boolean)this.remember.get() && !App.aim().hot()) {
                return mc.crosshairTarget instanceof EntityHitResult entityhitresult
                        && entityhitresult.getEntity() instanceof LivingEntity livingentity1
                        && livingentity1 != mc.player
                        && livingentity1.isAlive()
                    ? livingentity1
                    : null;
            } else {
                return livingentity;
            }
        } else {
            return null;
        }
    }

    private static boolean visible(MinecraftClient mc, LivingEntity target) {
        return mc.player != null
            && target != null
            && target.isAlive()
            && target.getEntityWorld() == mc.world
            && WorldVis.canSee(mc.player, target);
    }

    private float deltaSeconds() {
        long i = System.nanoTime();
        if (this.frameNanos == 0L) {
            this.frameNanos = i;
            return 0.016666668F;
        } else {
            float f = (float)(i - this.frameNanos) / 1.0E9F;
            this.frameNanos = i;
            return MathHelper.clamp(f, 0.0027777778F, 0.06666667F);
        }
    }

    private void clearImmediately() {
        this.shown = null;
        this.appear.snap(0.0F);
        this.followReady = false;
        this.boxLive = false;
        this.boxA = 0.0F;
    }

    private static double wrap(double value) {
        value %= 1.0;
        return value < 0.0 ? value + 1.0 : value;
    }

    @Override
    protected void disable() {
        this.clearImmediately();
        this.phase = 0.0;
        this.frameNanos = 0L;
    }

    public void setStyle(String style) {
        if (style != null && !style.isBlank()) {
            this.kind.set(STYLE_BOX.equalsIgnoreCase(style) ? STYLE_BOX : STYLE_CIRCLE.equalsIgnoreCase(style) ? STYLE_CIRCLE : STYLE_GHOST);
        }
    }

    public String style() {
        return (String)this.kind.get();
    }

    public LivingEntity current() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        LivingEntity livingentity = this.select(minecraftclient);
        return visible(minecraftclient, livingentity) ? livingentity : null;
    }

    public int tint() {
        return this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.color.get();
    }
}
