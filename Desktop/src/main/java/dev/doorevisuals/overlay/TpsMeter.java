package dev.doorevisuals.overlay;

import net.minecraft.client.MinecraftClient;

public final class TpsMeter {
    private static long lastGameTime = -1L;
    private static long lastNanos;
    private static float tps = 20.0F;

    private TpsMeter() {
    }

    public static void tick(MinecraftClient mc) {
        if (mc != null && mc.world != null) {
            long i = mc.world.getTime();
            long j = System.nanoTime();
            if (lastGameTime < 0L || lastNanos <= 0L) {
                lastGameTime = i;
                lastNanos = j;
            } else if (i != lastGameTime) {
                long k = i - lastGameTime;
                double d0 = (j - lastNanos) / 1.0E9;
                if (d0 > 0.02 && k > 0L) {
                    float f = (float)(k / d0);
                    f = Math.max(0.0F, Math.min(20.5F, f));
                    tps = tps * 0.82F + f * 0.18F;
                }

                lastGameTime = i;
                lastNanos = j;
            } else if (j - lastNanos > 400000000L) {
                tps = Math.max(0.0F, tps * 0.92F);
                lastNanos = j;
            }
        } else {
            tps = 20.0F;
            lastGameTime = -1L;
        }
    }

    public static float tps() {
        return tps;
    }
}
