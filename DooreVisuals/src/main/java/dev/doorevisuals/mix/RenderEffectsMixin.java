package dev.doorevisuals.mix;

import dev.doorevisuals.draw.MotionBlurFx;
import dev.doorevisuals.tools.BlurSystemFeature;
import dev.doorevisuals.tools.MotionBlurFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Screen-space effects applied right after the world is rendered and before the GUI, so the HUD stays sharp: the camera
 * motion blur, and the built-in menu blur used behind screens.
 */
@Mixin(GameRenderer.class)
public abstract class RenderEffectsMixin {
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
            shift = At.Shift.AFTER
        )
    )
    private void doore$blur(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            GameRenderer self = (GameRenderer)(Object)this;
            float[] motion = MotionBlurFeature.vector(mc, self.getCamera());
            if (motion != null) {
                MotionBlurFx.frame(motion[0], motion[1], MotionBlurFeature.samples());
            }
            int passes = BlurSystemFeature.shouldBlur(mc) ? BlurSystemFeature.passes() : 0;
            for (int i = 0; i < passes; i++) {
                self.renderBlur();
            }
        } catch (Throwable ignored) {
            // The feature is purely visual; a missing post processor must never break rendering.
        }
    }
}
