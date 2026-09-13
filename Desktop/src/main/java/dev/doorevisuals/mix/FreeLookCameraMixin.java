package dev.doorevisuals.mix;

import dev.doorevisuals.tools.FreeLookFeature;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FreeLookCameraMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 0))
    private void doore$rot0(Camera self, float yRot, float xRot) {
        apply(self, yRot, xRot);
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 1))
    private void doore$rot1(Camera self, float yRot, float xRot) {
        apply(self, yRot, xRot);
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void doore$freeLookReturn(World level, Entity entity, boolean detached, boolean mirrored, float partial, CallbackInfo ci) {
        if (FreeLookFeature.active() && detached && mirrored) {
            ((CameraAccessor)(Object)this).doore$setRotation(FreeLookFeature.yaw() + 180.0F, -FreeLookFeature.pitch());
        }
    }

    private static void apply(Camera self, float yRot, float xRot) {
        if (FreeLookFeature.active()) {
            ((CameraAccessor)self).doore$setRotation(FreeLookFeature.yaw(), FreeLookFeature.pitch());
        } else {
            ((CameraAccessor)self).doore$setRotation(yRot, xRot);
        }
    }
}
