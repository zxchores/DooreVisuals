package dev.doorevisuals.mix;

import dev.doorevisuals.App;
import dev.doorevisuals.tools.AspectFeature;
import dev.doorevisuals.tools.AspectRatio;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class RatioMixin {
    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void doore$beginAspect(RenderTickCounter deltaTracker, CallbackInfo ci) {
        if (App.live()) {
            App.features().find(AspectFeature.class).ifPresent(AspectRatio::sync);
        } else {
            AspectRatio.clear();
        }

        AspectRatio.setWorldPass(true);
    }

    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void doore$endAspect(RenderTickCounter deltaTracker, CallbackInfo ci) {
        AspectRatio.setWorldPass(false);
    }

    @ModifyArg(method = "getBasicProjectionMatrix", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"), index = 1)
    private float doore$aspect(float aspect) {
        return AspectRatio.resolve(aspect);
    }
}
