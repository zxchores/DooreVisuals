package dev.doorevisuals.mix;

import dev.doorevisuals.friends.FriendsFeature;
import dev.doorevisuals.friends.FriendsArmorTint;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorFeatureRenderer.class)
public abstract class FriendsArmorMixin {
    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
        at = @At("HEAD")
    )
    private void doore$begin(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state, float a, float b, CallbackInfo ci) {
        if (state instanceof PlayerEntityRenderState playerentityrenderstate) {
            int i = FriendsFeature.armorColor(playerentityrenderstate.id);
            if (i != -1) {
                FriendsArmorTint.TINT.set(i);
                return;
            }
        }

        FriendsArmorTint.TINT.remove();
    }

    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
        at = @At("TAIL")
    )
    private void doore$end(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state, float a, float b, CallbackInfo ci) {
        FriendsArmorTint.TINT.remove();
    }

    @Mixin(EquipmentRenderer.class)
    public static class Dye {
        @Inject(
            method = "getDyeColor(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$Layer;I)I",
            at = @At("RETURN"),
            cancellable = true
        )
        private static void doore$dye(CallbackInfoReturnable<Integer> cir) {
            Integer integer = FriendsArmorTint.TINT.get();
            if (integer != null) {
                int i = cir.getReturnValue() == null ? -1 : cir.getReturnValue();
                cir.setReturnValue(ColorHelper.mix(i == 0 ? -1 : i, integer | 0xFF000000));
            }
        }
    }
}
