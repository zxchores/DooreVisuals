package dev.doorevisuals.audio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

public final class Sfx {
    private Sfx() {
    }

    public static void play(SoundEvent event, float volume, float pitch) {
        if (event != null) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.getSoundManager() != null) {
                minecraftclient.getSoundManager().play(PositionedSoundInstance.ui(event, pitch, volume));
            }
        }
    }

    public static void playWorld(SoundEvent event, double x, double y, double z, float volume, float pitch) {
        if (event != null) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.world != null) {
                minecraftclient.world.playSoundClient(x, y, z, event, SoundCategory.PLAYERS, volume, pitch, false);
            }
        }
    }
}
