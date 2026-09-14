package dev.doorevisuals.mix;

import dev.doorevisuals.tools.PvpSaveFeature;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class PvpSaveMixin {
    @Inject(method = "disconnectWithProgressScreen()V", at = @At("HEAD"), cancellable = true)
    private void doore$hold(CallbackInfo ci) {
        if (PvpSaveFeature.blockDisconnect()) {
            ci.cancel();
        }
    }

    @Inject(method = "disconnectWithProgressScreen(Z)V", at = @At("HEAD"), cancellable = true)
    private void doore$holdFlag(boolean stopSounds, CallbackInfo ci) {
        if (PvpSaveFeature.blockDisconnect()) {
            ci.cancel();
        }
    }

    @Inject(method = "disconnectWithSavingScreen()V", at = @At("HEAD"), cancellable = true)
    private void doore$holdSave(CallbackInfo ci) {
        if (PvpSaveFeature.blockDisconnect()) {
            ci.cancel();
        }
    }
}
