package dev.doorevisuals.mix;

import dev.doorevisuals.tools.ContainerChrome;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementsScreen.class)
public class AdvancementsTintMixin {
    @Inject(method = "drawWindow", at = @At("HEAD"))
    private void doore$push(DrawContext g, int x, int y, int width, int height, CallbackInfo ci) {
        ContainerChrome.push();
    }

    @Inject(method = "drawWindow", at = @At("TAIL"))
    private void doore$pop(DrawContext g, int x, int y, int width, int height, CallbackInfo ci) {
        ContainerChrome.pop();
    }
}
