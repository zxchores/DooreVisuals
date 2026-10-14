package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.compat.IrisCompat;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;

/**
 * Camera motion blur. The blur length follows how fast the camera turns, multiplied by a fixed virtual exposure, so it
 * looks the same at 60 and at 360 FPS: on a high refresh rate a single frame moves very little, but the blur does not
 * shrink with it. The actual drawing is {@code draw/MotionBlurFx}.
 */
public final class MotionBlurFeature extends Feature {
    private static final float MAX_BLUR_UV = 0.12F;
    private static final long IRIS_CHECK_MS = 1000L;

    private final Opt.Num exposure = this.opt(new Opt.Num("exposure", "Сила (выдержка)", 0.6, 0.1, 1.5, 0.05));
    private final Opt.Pick quality = this.opt(new Opt.Pick("quality", "Качество", "Обычно", "Быстро", "Обычно", "Высокое"));
    private final Opt.Num minSpeed = this.opt(new Opt.Num("min_speed", "Порог (град/с)", 25.0, 0.0, 200.0, 5.0));
    private final Opt.Flag skipShaders = this.opt(new Opt.Flag("skip_shaders", "Отключать с шейдерами Iris", true));

    private static volatile boolean live;

    private float lastYaw;
    private float lastPitch;
    private long lastNanos;
    private boolean seeded;
    private float smoothX;
    private float smoothY;
    private long irisCheckedAt;
    private boolean irisOn;

    public MotionBlurFeature() {
        super("motion_blur", "Motion Blur", "Размытие мира при повороте камеры, не зависит от частоты кадров", Category.WORLD, false);
    }

    public static boolean active() {
        return live;
    }

    /** Number of taps for the chosen quality. */
    public static int samples() {
        return App.features().find(MotionBlurFeature.class).map(f -> switch (f.quality.get()) {
            case "Быстро" -> 12;
            case "Высокое" -> 32;
            default -> 20;
        }).orElse(20);
    }

    /**
     * Blur vector in uv units for this frame, or null when nothing should be blurred. Call once per frame from the point
     * where the world has just been rendered.
     */
    public static float[] vector(MinecraftClient mc, Camera camera) {
        if (!live || mc == null || mc.player == null || camera == null || mc.currentScreen != null) {
            return null;
        }
        MotionBlurFeature feature = App.features().find(MotionBlurFeature.class).orElse(null);
        return feature == null ? null : feature.compute(mc, camera);
    }

    private float[] compute(MinecraftClient mc, Camera camera) {
        long now = System.nanoTime();
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        float dt = (now - this.lastNanos) / 1.0E9F;
        boolean fresh = !this.seeded || dt <= 0.0F || dt > 0.25F;
        float dyaw = wrapDegrees(yaw - this.lastYaw);
        float dpitch = pitch - this.lastPitch;
        this.lastYaw = yaw;
        this.lastPitch = pitch;
        this.lastNanos = now;
        this.seeded = true;
        // First frame, a pause, or a teleport: no sensible speed to measure.
        if (fresh || Math.abs(dyaw) > 120.0F || Math.abs(dpitch) > 90.0F) {
            this.smoothX = 0.0F;
            this.smoothY = 0.0F;
            return null;
        }
        if (this.skipShaders.get() && this.shaderPackActive()) {
            return null;
        }

        float wy = dyaw / dt;
        float wp = dpitch / dt;
        if ((float)Math.hypot(wy, wp) < this.minSpeed.f()) {
            this.smoothX *= 0.5F;
            this.smoothY *= 0.5F;
            return this.finish();
        }

        float fov = (float)mc.options.getFov().getValue();
        float aspect = (float)mc.getWindow().getFramebufferWidth() / Math.max(1, mc.getWindow().getFramebufferHeight());
        float hFov = (float)Math.toDegrees(2.0 * Math.atan(Math.tan(Math.toRadians(fov) / 2.0) * aspect));
        float shutter = this.exposure.f() / 60.0F;
        // Angular speed over the field of view is "screens per second"; times the exposure it is the blur length.
        float targetX = -wy / Math.max(1.0F, hFov) * shutter;
        float targetY = wp / Math.max(1.0F, fov) * shutter;
        float ease = 1.0F - (float)Math.exp(-dt * 40.0F);
        this.smoothX += (targetX - this.smoothX) * ease;
        this.smoothY += (targetY - this.smoothY) * ease;
        return this.finish();
    }

    private float[] finish() {
        float length = (float)Math.hypot(this.smoothX, this.smoothY);
        if (length < 0.0008F) {
            return null;
        }
        float k = length > MAX_BLUR_UV ? MAX_BLUR_UV / length : 1.0F;
        return new float[] {this.smoothX * k, this.smoothY * k};
    }

    private boolean shaderPackActive() {
        long now = System.currentTimeMillis();
        if (now - this.irisCheckedAt > IRIS_CHECK_MS) {
            this.irisCheckedAt = now;
            this.irisOn = IrisCompat.shaderPackInUse();
        }
        return this.irisOn;
    }

    @Override
    protected void enable() {
        live = true;
        this.seeded = false;
    }

    @Override
    protected void disable() {
        live = false;
        this.seeded = false;
        this.smoothX = 0.0F;
        this.smoothY = 0.0F;
    }

    private static float wrapDegrees(float v) {
        v %= 360.0F;
        if (v >= 180.0F) {
            v -= 360.0F;
        }
        if (v < -180.0F) {
            v += 360.0F;
        }
        return v;
    }
}
