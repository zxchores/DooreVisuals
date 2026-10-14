package dev.doorevisuals.mix;

import dev.doorevisuals.world.HitColorFeature;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "getOverlay", at = @At("HEAD"), cancellable = true)
    private static void doore$hitColorOverlay(LivingEntityRenderState state, float whiteOverlayProgress, CallbackInfoReturnable<Integer> cir) {
        if (HitColorFeature.active() && state.hurt) {
            cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU(whiteOverlayProgress), OverlayTexture.getV(false)));
        }
    }

    @Inject(method = "getMixColor", at = @At("RETURN"), cancellable = true)
    private void doore$hitColorTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (state.hurt && HitColorFeature.active()) {
            int i = HitColorFeature.modelTint();
            if (i != -1) {
                cir.setReturnValue(ColorHelper.mix((Integer)cir.getReturnValue(), i));
            }
        }
    }
}
