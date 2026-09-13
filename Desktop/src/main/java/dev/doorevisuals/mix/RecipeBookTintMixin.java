package dev.doorevisuals.mix;

import dev.doorevisuals.tools.ContainerChrome;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookWidget.class)
public class RecipeBookTintMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void doore$push(DrawContext g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ContainerChrome.push();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void doore$pop(DrawContext g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ContainerChrome.pop();
    }
}
