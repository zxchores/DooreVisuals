package dev.doorevisuals.audio;

import dev.doorevisuals.tools.GuiWindowsFeature;
import net.minecraft.sound.SoundEvent;

public final class GuiSfx {
    private GuiSfx() {
    }

    public static void open() {
        play(ModSounds.GUI_OPEN, 0.85F, 1.0F);
    }

    public static void close() {
        play(ModSounds.GUI_CLOSE, 0.8F, 1.0F);
    }

    public static void toggle(boolean on) {
        play(on ? ModSounds.GUI_ON : ModSounds.GUI_OFF, 0.7F, 1.0F);
    }

    public static void click() {
        play(ModSounds.GUI_CLICK, 0.55F, 1.0F);
    }

    private static void play(SoundEvent event, float volume, float pitch) {
        if (GuiWindowsFeature.soundOn()) {
            Sfx.play(event, volume, pitch);
        }
    }
}
