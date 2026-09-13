package dev.doorevisuals.mix;

import dev.doorevisuals.tools.ItemPhysicsFeature;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityMixin {
    @Unique
    private static final ThreadLocal<Boolean> DOORE$GROUND = ThreadLocal.withInitial(() -> false);

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void doore$extract(ItemEntity entity, ItemEntityRenderState state, float partial, CallbackInfo ci) {
        boolean flag = ItemPhysicsFeature.active()
            && (entity.isOnGround() || entity.getVelocity().horizontalLengthSquared() < 1.0E-6 && Math.abs(entity.getVelocity().y) < 0.08);
        DOORE$GROUND.set(flag);
        if (flag) {
            float f = 20.0F + (entity.getId() & 1023) * 0.05F;
            if (ItemPhysicsFeature.freezeBob() || ItemPhysicsFeature.freezeSpin()) {
                state.age = f;
            }
        }
    }

    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionfc;)V", shift = Shift.AFTER)
    )
    private void doore$tilt(ItemEntityRenderState state, MatrixStack poseStack, OrderedRenderCommandQueue collector, CameraRenderState camera, CallbackInfo ci) {
        if (Boolean.TRUE.equals(DOORE$GROUND.get()) && ItemPhysicsFeature.active()) {
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ItemPhysicsFeature.tilt()));
        }
    }
}
