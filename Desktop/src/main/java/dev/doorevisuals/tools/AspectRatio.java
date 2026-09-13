package dev.doorevisuals.tools;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public final class AspectRatio {
    private static volatile boolean enabled;
    private static volatile boolean nativePreset;
    private static volatile double target = 1.7777777777777777;
    private static volatile boolean bars;
    private static volatile boolean worldPass;
    private static volatile String barStyle = "Black";

    private AspectRatio() {
    }

    public static void sync(AspectFeature feature) {
        if (feature != null && feature.on()) {
            enabled = true;
            nativePreset = feature.nativePreset();
            target = feature.targetRatio();
            bars = feature.bars() && !nativePreset;
            barStyle = feature.barStyle();
        } else {
            clear();
        }
    }

    public static void clear() {
        enabled = false;
        nativePreset = true;
        bars = false;
        worldPass = false;
    }

    public static void setWorldPass(boolean active) {
        worldPass = active;
    }

    public static boolean shouldOverride() {
        return enabled && worldPass && !nativePreset;
    }

    public static float resolve(float fallback) {
        return !shouldOverride() ? fallback : (float)Math.max(0.25, Math.min(4.0, target));
    }

    public static boolean showBars() {
        return enabled && bars && !nativePreset;
    }

    public static String barStyle() {
        return barStyle;
    }

    public static AspectRatio.Letterbox letterbox(int gw, int gh) {
        if (showBars() && gw > 0 && gh > 0) {
            double d0 = (double)gw / gh;
            if (Math.abs(target - d0) < 0.015) {
                return null;
            } else if (d0 > target) {
                int k = Math.max(1, (int)Math.round(gh * target));
                int l = Math.max(0, (gw - k) / 2);
                return l < 2 ? null : new AspectRatio.Letterbox(l, 0, k, gh, true);
            } else {
                int i = Math.max(1, (int)Math.round(gw / target));
                int j = Math.max(0, (gh - i) / 2);
                return j < 2 ? null : new AspectRatio.Letterbox(0, j, gw, i, false);
            }
        } else {
            return null;
        }
    }

    public static float nativeAspect() {
        Window window = MinecraftClient.getInstance().getWindow();
        return (float)Math.max(1, window.getFramebufferWidth()) / Math.max(1, window.getFramebufferHeight());
    }

    public record Letterbox(int x, int y, int w, int h, boolean vertical) {
    }
}
