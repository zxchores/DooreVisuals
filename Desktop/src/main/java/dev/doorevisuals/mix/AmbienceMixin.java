package dev.doorevisuals.mix;

import dev.doorevisuals.world.AmbienceFeature;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AtmosphericFogModifier.class)
public class AmbienceMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private void doore$ambience(ClientWorld level, Camera camera, int viewDistance, float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (AmbienceFeature.active()) {
            cir.setReturnValue(AmbienceFeature.applyArgb((Integer)cir.getReturnValue()));
        }
    }
}
