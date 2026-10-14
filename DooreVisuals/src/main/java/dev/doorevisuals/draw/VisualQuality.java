package dev.doorevisuals.draw;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public final class VisualQuality {
    private static volatile VisualQuality.Level level = VisualQuality.Level.HIGH;
    private static int frameBudget;

    private VisualQuality() {
    }

    public static void set(VisualQuality.Level l) {
        level = l == null ? VisualQuality.Level.MEDIUM : l;
    }

    public static void setLabel(String label) {
        String s = label == null ? "" : label;

        set(switch (s) {
            case "\u041d\u0438\u0437\u043a\u043e\u0435" -> VisualQuality.Level.LOW;
            case "\u0421\u0440\u0435\u0434\u043d\u0435\u0435" -> VisualQuality.Level.MEDIUM;
            default -> VisualQuality.Level.HIGH;
        });
    }

    public static VisualQuality.Level level() {
        return level;
    }

    public static String costHint() {
        return switch (level) {
            case LOW -> "\u043c\u0435\u043d\u044c\u0448\u0435 \u0447\u0430\u0441\u0442\u0438\u0446 \u00b7 cull ~28\u043c \u00b7 \u0431\u0435\u0437 soft-orb";
            case MEDIUM -> "\u0431\u0430\u043b\u0430\u043d\u0441 \u00b7 cull ~48\u043c \u00b7 2 \u0441\u043b\u043e\u044f \u043e\u0440\u0431\u043e\u0432";
            case HIGH -> "\u043c\u0430\u043a\u0441. \u043a\u0430\u0447\u0435\u0441\u0442\u0432\u043e \u00b7 cull ~72\u043c \u00b7 \u043f\u043e\u043b\u043d\u044b\u0439 budget";
        };
    }

    public static int segs(int high) {
        return switch (level) {
            case LOW -> Math.max(12, high * 2 / 5);
            case MEDIUM -> Math.max(20, high * 3 / 4);
            case HIGH -> high;
        };
    }

    public static int tubeSegs(int high) {
        return switch (level) {
            case LOW -> Math.max(6, high / 2);
            case MEDIUM -> Math.max(8, high * 3 / 4);
            case HIGH -> high;
        };
    }

    public static float particleMul() {
        return switch (level) {
            case LOW -> 0.45F;
            case MEDIUM -> 0.75F;
            case HIGH -> 1.0F;
        };
    }

    public static float maxFxDist() {
        return switch (level) {
            case LOW -> 28.0F;
            case MEDIUM -> 48.0F;
            case HIGH -> 72.0F;
        };
    }

    public static float maxFxDistSq() {
        float f = maxFxDist();
        return f * f;
    }

    public static boolean softOrbs() {
        return level != VisualQuality.Level.LOW;
    }

    public static int orbLayers() {
        return switch (level) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
        };
    }

    public static int orbSegs() {
        return switch (level) {
            case LOW -> 12;
            case MEDIUM -> 20;
            case HIGH -> 28;
        };
    }

    public static int frameParticleCap() {
        return switch (level) {
            case LOW -> 80;
            case MEDIUM -> 180;
            case HIGH -> 320;
        };
    }

    public static void beginFrame() {
        frameBudget = frameParticleCap();
    }

    public static boolean takeParticle() {
        if (frameBudget <= 0) {
            return false;
        } else {
            frameBudget--;
            return true;
        }
    }

    public static boolean inFrustumDist(double x, double y, double z) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.player == null) {
            return true;
        } else {
            Vec3d vec3d = minecraftclient.player.getCameraPosVec(1.0F);
            double d0 = x - vec3d.x;
            double d1 = y - vec3d.y;
            double d2 = z - vec3d.z;
            return d0 * d0 + d1 * d1 + d2 * d2 <= maxFxDistSq();
        }
    }

    public static enum Level {
        LOW,
        MEDIUM,
        HIGH;
    }
}
