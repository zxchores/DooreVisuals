package dev.doorevisuals.world;

import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.tools.TimeWeatherFeature;
import org.joml.Vector4f;

public final class AmbienceFeature {
    private static volatile boolean live;
    private static volatile float mix = 0.45F;
    private static volatile float cr = 0.25F;
    private static volatile float cg = 0.75F;
    private static volatile float cb = 0.63F;
    private static volatile float dawnR = 1.0F;
    private static volatile float dawnG = 0.55F;
    private static volatile float dawnB = 0.35F;
    private static volatile float duskR = 0.75F;
    private static volatile float duskG = 0.35F;
    private static volatile float duskB = 0.85F;
    private static volatile boolean timeBlend = true;
    private static volatile String timePresetLabel = "\u0412\u0430\u043d\u0438\u043b\u044c";
    private static volatile String weatherLookLabel = "\u0412\u0430\u043d\u0438\u043b\u044c";

    private AmbienceFeature() {
    }

    public static boolean active() {
        return live;
    }

    public static void applyColor(Vector4f vanilla) {
        if (live && vanilla != null) {
            float[] afloat = currentRgb();
            float f = mix;
            vanilla.x = vanilla.x * (1.0F - f) + afloat[0] * f;
            vanilla.y = vanilla.y * (1.0F - f) + afloat[1] * f;
            vanilla.z = vanilla.z * (1.0F - f) + afloat[2] * f;
        }
    }

    public static int applyArgb(int argb) {
        if (!live) {
            return argb;
        } else {
            float f = (argb >>> 24 & 0xFF) / 255.0F;
            float f1 = (argb >>> 16 & 0xFF) / 255.0F;
            float f2 = (argb >>> 8 & 0xFF) / 255.0F;
            float f3 = (argb & 0xFF) / 255.0F;
            float[] afloat = currentRgb();
            float f4 = mix;
            f1 = f1 * (1.0F - f4) + afloat[0] * f4;
            f2 = f2 * (1.0F - f4) + afloat[1] * f4;
            f3 = f3 * (1.0F - f4) + afloat[2] * f4;
            return Math.round(f * 255.0F) << 24 | Math.round(f1 * 255.0F) << 16 | Math.round(f2 * 255.0F) << 8 | Math.round(f3 * 255.0F);
        }
    }

    private static float[] currentRgb() {
        if (!timeBlend) {
            return new float[]{cr, cg, cb};
        } else {
            float f = dayFactor();
            float f1 = sunPhase();
            float f2;
            float f3;
            float f4;
            if (f1 < 0.5F) {
                float f5 = f1 * 2.0F;
                f2 = lerp(dawnR, cr, f5);
                f3 = lerp(dawnG, cg, f5);
                f4 = lerp(dawnB, cb, f5);
            } else {
                float f6 = (f1 - 0.5F) * 2.0F;
                f2 = lerp(cr, duskR, f6);
                f3 = lerp(cg, duskG, f6);
                f4 = lerp(cb, duskB, f6);
            }

            f2 = lerp(f2, cr, f * 0.35F);
            f3 = lerp(f3, cg, f * 0.35F);
            f4 = lerp(f4, cb, f * 0.35F);
            float[] afloat = weatherTint();
            return new float[]{f2 * afloat[0], f3 * afloat[1], f4 * afloat[2]};
        }
    }

    private static float dayFactor() {
        float f = sunPhase();
        float f1 = (float)Math.cos((f - 0.25) * Math.PI * 2.0);
        return clamp01(f1 * 0.5F + 0.5F);
    }

    private static float sunPhase() {
        String s = timePresetLabel;

        return switch (s) {
            case "\u0420\u0430\u0441\u0441\u0432\u0435\u0442" -> 0.05F;
            case "\u041f\u043e\u043b\u0434\u0435\u043d\u044c" -> 0.35F;
            case "\u0417\u0430\u043a\u0430\u0442" -> 0.72F;
            case "\u041f\u043e\u043b\u043d\u043e\u0447\u044c" -> 0.92F;
            default -> vanillaSunPhase();
        };
    }

    private static float vanillaSunPhase() {
        return (float)((TimeWeatherFeature.visualTime() % 24000L + 24000L) % 24000L) / 24000.0F;
    }

    private static float[] weatherTint() {
        String s = weatherLookLabel;

        return switch (s) {
            case "\u042f\u0441\u043d\u043e" -> new float[]{1.05F, 1.02F, 0.95F};
            case "\u0414\u043e\u0436\u0434\u044c" -> new float[]{0.65F, 0.72F, 0.85F};
            case "\u0422\u0443\u043c\u0430\u043d" -> new float[]{0.78F, 0.8F, 0.82F};
            default -> new float[]{1.0F, 1.0F, 1.0F};
        };
    }

    private static float lerp(float a, float b, float t) {
        t = clamp01(t);
        return a + (b - a) * t;
    }

    private static float clamp01(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    public static void applyLive(boolean on, float strength, boolean blend, int dayArgb, int dawnArgb, int duskArgb, String timePreset, String weatherLook) {
        live = on;
        mix = strength;
        timeBlend = blend;
        timePresetLabel = timePreset == null ? "\u0412\u0430\u043d\u0438\u043b\u044c" : timePreset;
        weatherLookLabel = weatherLook == null ? "\u0412\u0430\u043d\u0438\u043b\u044c" : weatherLook;
        unpack(dayArgb);
        dawnR = (dawnArgb >> 16 & 0xFF) / 255.0F;
        dawnG = (dawnArgb >> 8 & 0xFF) / 255.0F;
        dawnB = (dawnArgb & 0xFF) / 255.0F;
        duskR = (duskArgb >> 16 & 0xFF) / 255.0F;
        duskG = (duskArgb >> 8 & 0xFF) / 255.0F;
        duskB = (duskArgb & 0xFF) / 255.0F;
    }

    private static void unpack(int c) {
        if (c == 0) {
            c = Theme.EFFECT;
        }

        cr = (c >> 16 & 0xFF) / 255.0F;
        cg = (c >> 8 & 0xFF) / 255.0F;
        cb = (c & 0xFF) / 255.0F;
    }
}
