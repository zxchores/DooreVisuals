package dev.doorevisuals.mix;

import dev.doorevisuals.tools.ZoomFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class ZoomScrollMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void doore$zoomWheel(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (ZoomFeature.held()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.currentScreen == null) {
                ZoomFeature.wheel(yOffset);
                ci.cancel();
            }
        }
    }
}
