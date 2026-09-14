package dev.doorevisuals.world;

import dev.doorevisuals.draw.Theme;
import net.minecraft.client.render.fog.FogData;
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
        if (live && vanilla != null && fogMix > 0.001F) {
            float f = Math.max(0.0F, Math.min(1.0F, fogMix));
            vanilla.x = vanilla.x * (1.0F - f) + fogR * f;
            vanilla.y = vanilla.y * (1.0F - f) + fogG * f;
            vanilla.z = vanilla.z * (1.0F - f) + fogB * f;
        }
    }

    public static float[] bufferDistances(float renderDistanceEnd) {
        float f = Math.max(48.0F, renderDistanceEnd);
        float f1 = Math.max(0.25F, dens);
        float f2 = Math.max(24.0F, f * endMul / f1);
        float f3 = Math.max(0.0F, f2 * Math.max(0.02F, startMul));
        if (f3 > f2 - 6.0F) {
            f3 = Math.max(0.0F, f2 - 6.0F);
        }

        return new float[]{f3, f2, Math.max(0.0F, f2 * 0.85F), Math.max(f2, f), f2, f2};
    }

    public static void applyLive(boolean on, float density, float start, float end, float mixAmt, int argb) {
        live = on;
        dens = density;
        startMul = start;
        endMul = end;
        fogMix = Math.max(0.0F, Math.min(1.0F, mixAmt));
        if (argb == 0) {
            argb = Theme.EFFECT;
        }

        fogR = (argb >> 16 & 0xFF) / 255.0F;
        fogG = (argb >> 8 & 0xFF) / 255.0F;
        fogB = (argb & 0xFF) / 255.0F;
    }
}
