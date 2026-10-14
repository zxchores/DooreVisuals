package dev.doorevisuals.mix;

import dev.doorevisuals.tools.FreeLookFeature;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mouse.class)
public class FreeLookMouseMixin {
    @Redirect(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void doore$freeLookTurn(ClientPlayerEntity player, double dx, double dy) {
        if (FreeLookFeature.active()) {
            FreeLookFeature.turn(dx, dy);
        } else {
            player.changeLookDirection(dx, dy);
        }
    }
}
