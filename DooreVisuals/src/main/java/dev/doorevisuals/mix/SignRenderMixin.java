package dev.doorevisuals.mix;

import dev.doorevisuals.tools.NoRenderFeature;
import net.minecraft.client.render.block.entity.AbstractSignBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignBlockEntityRenderer.class)
public class SignRenderMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void doore$signs(CallbackInfo ci) {
        if (NoRenderFeature.signs()) {
            ci.cancel();
        }
    }
}
