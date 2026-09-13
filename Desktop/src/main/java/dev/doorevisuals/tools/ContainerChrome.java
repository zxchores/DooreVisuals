package dev.doorevisuals.tools;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ColorHelper;

public final class ContainerChrome {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final int MUL = -14012360;

    private ContainerChrome() {
    }

    public static void push() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void pop() {
        DEPTH.set(Math.max(0, DEPTH.get() - 1));
    }

    public static void reset() {
        DEPTH.set(0);
    }

    public static boolean active() {
        return DEPTH.get() > 0;
    }

    public static int tint(int color) {
        if (!active()) {
            return color;
        } else {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            return minecraftclient.currentScreen == null ? color : ColorHelper.mix(color, -14012360);
        }
    }
}
