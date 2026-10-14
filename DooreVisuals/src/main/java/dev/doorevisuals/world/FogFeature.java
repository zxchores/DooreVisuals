package dev.doorevisuals.world;

import dev.doorevisuals.draw.Theme;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.MinecraftClient;
import org.joml.Vector4f;

public final class FogFeature {
    private static volatile boolean live;
    private static volatile float dens = 1.15F;
    private static volatile float startMul = 0.22F;
    private static volatile float endMul = 0.72F;
    private static volatile float fogR = 0.45F;
    private static volatile float fogG = 0.55F;
    private static volatile float fogB = 0.62F;
    private static volatile float fogMix = 0.55F;
    private static volatile float targetDens = 1.15F;
    private static volatile float targetStartMul = 0.22F;
    private static volatile float targetEndMul = 0.72F;
    private static volatile float targetFogR = 0.45F;
    private static volatile float targetFogG = 0.55F;
    private static volatile float targetFogB = 0.62F;
    private static volatile float targetFogMix = 0.55F;
    private static long lastUpdate = System.nanoTime();

    private FogFeature() {
    }

    public static boolean active() {
        return live;
    }

    public static void applyDistance(FogData data, float renderDistanceBlocks) {
        if (live && data != null) {
            float[] afloat = bufferDistances(renderDistanceBlocks);
            data.environmentalStart = afloat[0];
            data.environmentalEnd = afloat[1];
            data.renderDistanceStart = afloat[2];
            data.renderDistanceEnd = afloat[3];
            data.skyEnd = afloat[4];
            data.cloudEnd = afloat[5];
        }
    }

    public static void applyColor(Vector4f vanilla) {
        advance();
        if (live && vanilla != null && fogMix > 0.001F) {
            float f = Math.max(0.0F, Math.min(1.0F, fogMix));
            vanilla.x = vanilla.x * (1.0F - f) + fogR * f;
            vanilla.y = vanilla.y * (1.0F - f) + fogG * f;
            vanilla.z = vanilla.z * (1.0F - f) + fogB * f;
        }
    }

    /** Цвет тумана из текущей палитры неба: туман следует за пресетом и временем суток. */
    public static void setTint(int argb) {
        targetFogR = (argb >> 16 & 0xFF) / 255.0F;
        targetFogG = (argb >> 8 & 0xFF) / 255.0F;
        targetFogB = (argb & 0xFF) / 255.0F;
    }

    /** Ниже уровня моря воздух плотнее, высоко в горах — прозрачнее. */
    private static float heightFactor() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.gameRenderer == null || mc.gameRenderer.getCamera() == null) {
            return 1.0F;
        }

        double y = mc.gameRenderer.getCamera().getCameraPos().y;
        return (float) Math.max(0.72, Math.min(1.35, 1.28 - (y - 40.0) * 0.004));
    }

    public static float[] bufferDistances(float renderDistanceEnd) {
        advance();
        float f = Math.max(48.0F, renderDistanceEnd);
        float f1 = Math.max(0.25F, dens * heightFactor());
        float f2 = Math.max(24.0F, f * endMul / f1);
        float f3 = Math.max(0.0F, f2 * Math.max(0.02F, startMul));
        if (f3 > f2 - 6.0F) {
            f3 = Math.max(0.0F, f2 - 6.0F);
        }

        return new float[]{f3, f2, Math.max(0.0F, f2 * 0.85F), Math.max(f2, f), f2, f2};
    }

    public static void applyLive(boolean on, float density, float start, float end, float mixAmt, int argb) {
        live = on;
        targetDens = density;
        targetStartMul = start;
        targetEndMul = end;
        targetFogMix = Math.max(0.0F, Math.min(1.0F, mixAmt));
        if (argb == 0) {
            argb = Theme.EFFECT;
        }

        targetFogR = (argb >> 16 & 0xFF) / 255.0F;
        targetFogG = (argb >> 8 & 0xFF) / 255.0F;
        targetFogB = (argb & 0xFF) / 255.0F;
        advance();
    }

    private static void advance() {
        long now = System.nanoTime();
        float dt = Math.max(0.0F, Math.min(0.1F, (now - lastUpdate) / 1.0E9F));
        lastUpdate = now;
        float t = 1.0F - (float)Math.exp(-dt / 0.55F);
        dens = lerp(dens, targetDens, t);
        startMul = lerp(startMul, targetStartMul, t);
        endMul = lerp(endMul, targetEndMul, t);
        fogR = lerp(fogR, targetFogR, t);
        fogG = lerp(fogG, targetFogG, t);
        fogB = lerp(fogB, targetFogB, t);
        fogMix = lerp(fogMix, targetFogMix, t);
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * amount;
    }
}
