package dev.doorevisuals.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.player.PlayerEntity;

public final class SessionStats {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static long playMs;
    private static int kills;
    private static int deaths;
    private static long lastTick;
    private static boolean wasDead;
    private static long lastWrite;

    private SessionStats() {
    }

    public static Path file() {
        return ClientPaths.root().resolve("session.json");
    }

    public static void tick(MinecraftClient mc) {
        long i = System.currentTimeMillis();
        if (lastTick == 0L) {
            lastTick = i;
            load();
        }

        if (mc.player != null && mc.world != null && mc.currentScreen == null) {
            playMs = playMs + Math.max(0L, Math.min(250L, i - lastTick));
        }

        lastTick = i;
        PlayerEntity playerentity = mc.player;
        boolean flag = playerentity != null && (playerentity.isDead() || mc.currentScreen instanceof DeathScreen);
        if (flag && !wasDead) {
            deaths++;
            flush();
        }

        wasDead = flag;
        if (i - lastWrite > 5000L) {
            flush();
        }
    }

    public static void kill() {
        kills++;
        flush();
    }

    public static long playMs() {
        return playMs;
    }

    public static int kills() {
        return kills;
    }

    public static int deaths() {
        return deaths;
    }

    public static String playLabel() {
        long i = playMs / 1000L;
        long j = i / 3600L;
        long k = i % 3600L / 60L;
        return j > 0L ? j + " \u0447 " + k + " \u043c\u0438\u043d" : k + " \u043c\u0438\u043d";
    }

    private static void load() {
        Path path = file();
        if (Files.isRegularFile(path)) {
            try {
                JsonObject jsonobject = (JsonObject)GSON.fromJson(Files.readString(path), JsonObject.class);
                if (jsonobject == null) {
                    return;
                }

                playMs = jsonobject.has("playMs") ? jsonobject.get("playMs").getAsLong() : 0L;
                kills = jsonobject.has("kills") ? jsonobject.get("kills").getAsInt() : 0;
                deaths = jsonobject.has("deaths") ? jsonobject.get("deaths").getAsInt() : 0;
            } catch (Throwable throwable) {
            }
        }
    }

    public static void flush() {
        lastWrite = System.currentTimeMillis();

        try {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("playMs", playMs);
            jsonobject.addProperty("kills", kills);
            jsonobject.addProperty("deaths", deaths);
            jsonobject.addProperty("playLabel", playLabel());
            Files.writeString(file(), GSON.toJson(jsonobject));
        } catch (Throwable throwable) {
        }
    }
}
