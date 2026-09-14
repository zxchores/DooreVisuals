package dev.doorevisuals.tools;

import net.minecraft.client.MinecraftClient;

public final class TimeWeatherFeature {
    private static volatile boolean timeLocked = false;
    private static volatile int lockedTime = 6000;
    private static volatile float timeScale = 1.0F;
    private static volatile String weatherMode = "default";
    private static volatile double fakeTime = 6000.0;
    private static volatile boolean useFake;
    private static final ThreadLocal<Boolean> RAW = ThreadLocal.withInitial(() -> false);

    private TimeWeatherFeature() {
    }

    public static boolean readingVanilla() {
        return Boolean.TRUE.equals(RAW.get());
    }

    public static boolean timeLocked() {
        return readingVanilla() ? false : timeLocked || useFake && timeScale != 1.0F;
    }

    public static int lockedTime() {
        return timeLocked ? lockedTime : (int)Math.floor(fakeTime);
    }

    public static float timeScale() {
        return timeScale;
    }

    public static String weatherMode() {
        return weatherMode;
    }

    public static long visualTime() {
        if (timeLocked()) {
            return lockedTime();
        } else {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.world == null) {
                return 6000L;
            } else {
                RAW.set(true);

                long i;
                try {
                    i = minecraftclient.world.getTimeOfDay();
                } finally {
                    RAW.set(false);
                }

                return i;
            }
        }
    }

    public static void applyLive(boolean on, boolean lock, int time, float speed, String weather) {
        if (!on) {
            timeLocked = false;
            timeScale = 1.0F;
            weatherMode = "default";
            useFake = false;
        } else {
            boolean flag = lock || Math.abs(speed - 1.0F) > 0.001F;
            if (!flag) {
                timeLocked = false;
                lockedTime = time;
                timeScale = 1.0F;
                weatherMode = weather == null ? "default" : weather.trim();
                useFake = false;
            } else {
                if (!useFake) {
                    seedFromWorld();
                    useFake = true;
                }

                timeLocked = lock;
                lockedTime = time;
                timeScale = speed;
                weatherMode = weather == null ? "default" : weather.trim();
                if (lock) {
                    fakeTime = time;
                    useFake = true;
                }
            }
        }
    }

    private static void seedFromWorld() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.world != null) {
            RAW.set(true);

            try {
                fakeTime = minecraftclient.world.getTimeOfDay() % 24000L;
            } finally {
                RAW.set(false);
            }
        }
    }

    public static void advance() {
        if (useFake && !timeLocked) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.world != null) {
                fakeTime = (fakeTime + timeScale) % 24000.0;
                if (fakeTime < 0.0) {
                    fakeTime += 24000.0;
                }
            }
        }
    }
}
