package dev.doorevisuals.mix;

import dev.doorevisuals.overlay.HudTweaksFeature;
import dev.doorevisuals.tools.ContainerChrome;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DrawContext.class)
public class GuiBlitTintMixin {
    @ModifyVariable(
        method = "drawTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lnet/minecraft/client/gl/GpuSampler;IIIIFFFFI)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 4
    )
    private int doore$blit(int color) {
        return HudTweaksFeature.sidebarTint(ContainerChrome.tint(color));
    }

    @ModifyVariable(
        method = "drawTiledTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lnet/minecraft/client/gl/GpuSampler;IIIIIIFFFFI)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 6
    )
    private int doore$tiled(int color) {
        return HudTweaksFeature.sidebarTint(ContainerChrome.tint(color));
    }

    @ModifyVariable(
        method = "fill(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/texture/TextureSetup;IIIIILjava/lang/Integer;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 4
    )
    private int doore$rect(int color) {
        return HudTweaksFeature.sidebarTint(ContainerChrome.tint(color));
    }
}
