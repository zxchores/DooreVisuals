package dev.doorevisuals.mix;

import dev.doorevisuals.tools.HandShaderFeature;
import dev.doorevisuals.tools.ItemAnimationsFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Item Animations in the held-item renderer: a faster swap with a transition, the eating and drinking poses, and the marker
 * the Hand Shader needs to know that the item being drawn right now is the one in the first-person hand.
 */
@Mixin(HeldItemRenderer.class)
public abstract class ItemAnimMixin {
    @Shadow
    private ItemStack mainHand;
    @Shadow
    private ItemStack offHand;
    @Shadow
    private float equipProgressMainHand;
    @Shadow
    private float equipProgressOffHand;

    /** The swap moves the extra share of the way to where it is going, on top of the game's own step. */
    @Inject(method = "updateHeldItems", at = @At("TAIL"))
    private void doore$fastSwap(CallbackInfo ci) {
        float k = ItemAnimationsFeature.swapNudge();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (k <= 0.0F || player == null) {
            return;
        }
        float mainTarget = ItemStack.areEqual(this.mainHand, player.getMainHandStack()) ? 1.0F : 0.0F;
        float offTarget = ItemStack.areEqual(this.offHand, player.getOffHandStack()) ? 1.0F : 0.0F;
        this.equipProgressMainHand = MathHelper.clamp(this.equipProgressMainHand + (mainTarget - this.equipProgressMainHand) * k, 0.0F, 1.0F);
        this.equipProgressOffHand = MathHelper.clamp(this.equipProgressOffHand + (offTarget - this.equipProgressOffHand) * k, 0.0F, 1.0F);
    }

    /** The eating and drinking pose: the game's own shape, scaled or reshaped by the chosen style. */
    @Inject(method = "applyEatOrDrinkTransformation", at = @At("HEAD"), cancellable = true)
    private void doore$eat(MatrixStack pose, float tickDelta, Arm arm, ItemStack stack, PlayerEntity player, CallbackInfo ci) {
        String style = ItemAnimationsFeature.eatStyle();
        if (ItemAnimationsFeature.EAT_VANILLA.equals(style)) {
            return;
        }
        ci.cancel();
        if (ItemAnimationsFeature.EAT_NONE.equals(style)) {
            return;
        }
        float power = ItemAnimationsFeature.eatPower();
        float left = player.getItemUseTimeLeft() - tickDelta + 1.0F;
        float progress = left / stack.getMaxUseTime(player);
        float bob = 1.0F;
        float move = 1.0F;
        float angle = 1.0F;
        switch (style) {
            case ItemAnimationsFeature.EAT_SOFT -> {
                bob = 0.45F;
                angle = 0.7F;
            }
            case ItemAnimationsFeature.EAT_STRONG -> {
                bob = 1.8F;
                angle = 1.3F;
            }
            case ItemAnimationsFeature.EAT_CENTER -> {
                move = 0.3F;
                angle = 0.5F;
            }
            default -> {
            }
        }
        if (progress < 0.8F) {
            pose.translate(0.0F, Math.abs(MathHelper.cos(left / 4.0F * (float)Math.PI) * 0.1F) * bob * power, 0.0F);
        }
        float h = 1.0F - (float)Math.pow(progress, 27.0);
        int side = arm == Arm.RIGHT ? 1 : -1;
        pose.translate(h * 0.6F * side * move * power, h * -0.5F * power, 0.0F);
        pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * h * 90.0F * angle * power));
        pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F * angle * power));
        pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * h * 30.0F * angle * power));
    }

    /** Marks the item of the first-person hand while the game draws it (the Hand Shader paints waves on it). */
    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
            at = @At("HEAD")
    )
    private void doore$itemBegin(LivingEntity entity, ItemStack stack, ItemDisplayContext context, MatrixStack matrices,
                                 OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        HandShaderFeature.beginItem(context);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
            at = @At("TAIL")
    )
    private void doore$itemEnd(LivingEntity entity, ItemStack stack, ItemDisplayContext context, MatrixStack matrices,
                               OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        HandShaderFeature.endItem();
    }
}
