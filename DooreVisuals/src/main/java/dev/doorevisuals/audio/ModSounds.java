package dev.doorevisuals.audio;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final SoundEvent GUI_OPEN = reg("gui_open");
    public static final SoundEvent GUI_CLOSE = reg("gui_close");
    public static final SoundEvent GUI_ON = reg("gui_on");
    public static final SoundEvent GUI_OFF = reg("gui_off");
    public static final SoundEvent GUI_CLICK = reg("gui_click");
    public static final SoundEvent HIT_CLICK = reg("hit_click");
    public static final SoundEvent HIT_THUD = reg("hit_thud");
    public static final SoundEvent HIT_CRYSTAL = reg("hit_crystal");
    public static final SoundEvent HIT_BLIP = reg("hit_blip");
    public static final SoundEvent HIT_POP = reg("hit_pop");
    public static final SoundEvent HIT_SNAP = reg("hit_snap");
    public static final SoundEvent HIT_KNOCK = reg("hit_knock");
    public static final SoundEvent HIT_TICK = reg("hit_tick");
    public static final SoundEvent HIT_BEM = reg("hit_bem");
    public static final SoundEvent KILL_BOOM = reg("kill_boom");
    public static final SoundEvent TOTEM_MELL = reg("totem_mell");

    private ModSounds() {
    }

    public static void boot() {
    }

    private static SoundEvent reg(String id) {
        Identifier identifier = Identifier.of("doorevisuals", id);
        return (SoundEvent)Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
    }
}
