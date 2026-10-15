package dev.doorevisuals.mix;

import dev.doorevisuals.ui.LoadingScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the client's loading screen over the game's splash once the game has drawn it; the game still runs the loading itself. */
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow
    private float progress;
    @Shadow
    private long reloadCompleteTime;

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"))
    private void doore$splash(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        LoadingScreen.draw(context, this.progress, this.reloadCompleteTime);
    }
}
