package dev.doorevisuals.mix;

import dev.doorevisuals.tools.ContainerChrome;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class ContainerTintMixin {
    @Inject(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V"))
    private void doore$push(DrawContext g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ContainerChrome.push();
    }

    @Inject(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V", shift = Shift.AFTER)
    )
    private void doore$pop(DrawContext g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ContainerChrome.pop();
    }
}
