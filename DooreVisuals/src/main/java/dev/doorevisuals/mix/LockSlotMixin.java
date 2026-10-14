package dev.doorevisuals.mix;

import dev.doorevisuals.tools.LockSlotFeature;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class LockSlotMixin {
    @Inject(method = "dropSelectedItem(Z)Z", at = @At("HEAD"), cancellable = true)
    private void doore$drop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (LockSlotFeature.blockDrop()) {
            cir.setReturnValue(false);
        }
    }

    @Mixin(ClientPlayerInteractionManager.class)
    public static class Click {
        @Inject(
            method = "clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V",
            at = @At("HEAD"),
            cancellable = true
        )
        private void doore$throw(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
            if (LockSlotFeature.blockClick(slotId, actionType)) {
                ci.cancel();
            }
        }
    }
}
