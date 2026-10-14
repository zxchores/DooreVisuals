package dev.doorevisuals.mix;

import dev.doorevisuals.tools.ZoomFeature;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class FovMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void doore$fov(Camera camera, float partialTick, boolean applyFovEffects, CallbackInfoReturnable<Float> cir) {
        float f = ZoomFeature.fovOverride();
        if (!Float.isNaN(f)) {
            cir.setReturnValue(f);
        }
    }
}
