package dev.doorevisuals.mix;

import dev.doorevisuals.tools.NoRenderFeature;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class CameraFxMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void doore$hurt(MatrixStack pose, float pt, CallbackInfo ci) {
        if (NoRenderFeature.hurtCam()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void doore$bob(MatrixStack pose, float pt, CallbackInfo ci) {
        if (NoRenderFeature.bobbing()) {
            ci.cancel();
        }
    }
}
