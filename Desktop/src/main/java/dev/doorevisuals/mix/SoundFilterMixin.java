package dev.doorevisuals.mix;

import dev.doorevisuals.tools.HitSoundFeature;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class SoundFilterMixin {
    @Inject(method = "playSoundClient(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V", at = @At("HEAD"), cancellable = true)
    private void doore$filter(
        double x, double y, double z, SoundEvent sound, SoundCategory source, float volume, float pitch, boolean distanceDelay, CallbackInfo ci
    ) {
        if (sound != null) {
            Identifier identifier = Registries.SOUND_EVENT.getId(sound);
            if (identifier != null && "minecraft".equals(identifier.getNamespace())) {
                String s = identifier.getPath();
                if (s.startsWith("entity.player.attack.") && HitSoundFeature.muteVanillaHits()) {
                    ci.cancel();
                } else {
                    if ("item.totem.use".equals(s) && HitSoundFeature.muteVanillaTotem()) {
                        ci.cancel();
                        HitSoundFeature.playTotem();
                    }
                }
            }
        }
    }
}
