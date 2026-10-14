package dev.doorevisuals.mix;

import dev.doorevisuals.tools.NoRenderFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class ScreenFxMixin {
    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void doore$water(MinecraftClient mc, MatrixStack pose, VertexConsumerProvider buffers, CallbackInfo ci) {
        if (NoRenderFeature.water()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void doore$fire(MatrixStack pose, VertexConsumerProvider buffers, Sprite sprite, CallbackInfo ci) {
        if (NoRenderFeature.fire()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void doore$block(Sprite sprite, MatrixStack pose, VertexConsumerProvider buffers, CallbackInfo ci) {
        if (NoRenderFeature.fire() || NoRenderFeature.blockInFace()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFloatingItem", at = @At("HEAD"), cancellable = true)
    private void doore$totem(MatrixStack pose, float pt, OrderedRenderCommandQueue collector, CallbackInfo ci) {
        if (NoRenderFeature.totem()) {
            ci.cancel();
        }
    }
}
