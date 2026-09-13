package dev.doorevisuals.mix;

import dev.doorevisuals.overlay.HudTweaksFeature;
import dev.doorevisuals.tools.ContainerChrome;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DrawContext.class)
public class GuiBlitTintMixin {
    @ModifyVariable(method = "drawTexturedQuad", at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private int doore$blit(int color) {
        return HudTweaksFeature.sidebarTint(ContainerChrome.tint(color));
    }

    @ModifyVariable(method = "drawTiledTexturedQuad", at = @At("HEAD"), argsOnly = true, ordinal = 6)
    private int doore$tiled(int color) {
        return HudTweaksFeature.sidebarTint(ContainerChrome.tint(color));
    }

    @ModifyVariable(method = "fill", at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private int doore$rect(int color) {
        return HudTweaksFeature.sidebarTint(ContainerChrome.tint(color));
    }
}
