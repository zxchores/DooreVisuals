package dev.doorevisuals.audio;

import dev.doorevisuals.tools.GuiWindowsFeature;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/** The sounds of the interface, in the pack chosen in GUI Windows (Стандарт, Мягкие, Кристалл, Клавиши, Тихие). */
public final class GuiSfx {
    private GuiSfx() {
    }

    public static void open() {
        play(Kind.OPEN);
    }

    public static void close() {
        play(Kind.CLOSE);
    }

    public static void toggle(boolean on) {
        play(on ? Kind.ON : Kind.OFF);
    }

    public static void click() {
        play(Kind.CLICK);
    }

    private enum Kind {
        OPEN(ModSounds.GUI_OPEN, 0.85F, 1.0F),
        CLOSE(ModSounds.GUI_CLOSE, 0.8F, 1.0F),
        ON(ModSounds.GUI_ON, 0.7F, 1.0F),
        OFF(ModSounds.GUI_OFF, 0.7F, 1.0F),
        CLICK(ModSounds.GUI_CLICK, 0.55F, 1.0F);

        final SoundEvent own;
        final float volume;
        final float pitch;

        Kind(SoundEvent own, float volume, float pitch) {
            this.own = own;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    private static void play(Kind kind) {
        if (!GuiWindowsFeature.soundOn()) {
            return;
        }
        switch (GuiWindowsFeature.soundPack()) {
            case "Мягкие" -> Sfx.play(kind.own, kind.volume * 0.6F, kind.pitch * 0.85F);
            case "Тихие" -> Sfx.play(kind.own, kind.volume * 0.25F, kind.pitch);
            case "Кристалл" -> Sfx.play(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.5F, switch (kind) {
                case OPEN -> 1.3F;
                case CLOSE -> 0.9F;
                case ON -> 1.6F;
                case OFF -> 1.0F;
                default -> 1.8F;
            });
            case "Клавиши" -> Sfx.play(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.6F, switch (kind) {
                case OPEN -> 1.4F;
                case CLOSE -> 0.8F;
                case ON -> 1.6F;
                case OFF -> 0.9F;
                default -> 1.2F;
            });
            default -> Sfx.play(kind.own, kind.volume, kind.pitch);
        }
    }
}
