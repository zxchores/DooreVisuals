package dev.doorevisuals.mix;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.overlay.HudFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class IslandMouseMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void doore$island(long window, MouseInput info, int action, CallbackInfo ci) {
        if (this.client != null && this.client.currentScreen == null && App.live() && action == 1) {
            if (info.button() == 0) {
                HudFeature hudfeature = App.features().find(HudFeature.class).filter(Feature::on).orElse(null);
                if (hudfeature != null) {
                    double d0 = this.client.mouse.getX()
                        * this.client.getWindow().getScaledWidth()
                        / Math.max(1, this.client.getWindow().getWidth());
                    double d1 = this.client.mouse.getY()
                        * this.client.getWindow().getScaledHeight()
                        / Math.max(1, this.client.getWindow().getHeight());
                    if (hudfeature.tryIslandClick(d0, d1)) {
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void doore$islandScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (this.client != null && this.client.currentScreen == null && App.live()) {
            HudFeature hudfeature = App.features().find(HudFeature.class).filter(Feature::on).orElse(null);
            if (hudfeature != null) {
                double d0 = this.client.mouse.getX()
                    * this.client.getWindow().getScaledWidth()
                    / Math.max(1, this.client.getWindow().getWidth());
                double d1 = this.client.mouse.getY()
                    * this.client.getWindow().getScaledHeight()
                    / Math.max(1, this.client.getWindow().getHeight());
                if (hudfeature.tryIslandScroll(d0, d1, yOffset)) {
                    ci.cancel();
                }
            }
        }
    }
}
