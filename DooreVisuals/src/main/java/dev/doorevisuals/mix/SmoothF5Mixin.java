package dev.doorevisuals.mix;

import dev.doorevisuals.tools.SmoothF5Feature;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class SmoothF5Mixin {
    @Unique
    private static Vec3d doore$lastThird;

    @Inject(method = "update", at = @At("RETURN"))
    private void doore$smoothF5(World level, Entity entity, boolean detached, boolean mirrored, float partial, CallbackInfo ci) {
        if (SmoothF5Feature.active() && entity != null) {
            float f = SmoothF5Feature.blend();
            Camera camera = (Camera)(Object)this;
            Vec3d vec3d = camera.getCameraPos();
            if (detached) {
                doore$lastThird = vec3d;
            }

            Vec3d vec3d1 = detached ? vec3d : doore$lastThird;
            if (vec3d1 != null) {
                Vec3d vec3d2 = entity.getCameraPosVec(partial);
                Vec3d vec3d3 = vec3d2.add(vec3d1.subtract(vec3d2).multiply(f));
                ((CameraAccessor)camera).doore$setPosition(vec3d3.x, vec3d3.y, vec3d3.z);
            }
        }
    }

    @Inject(method = "isThirdPerson", at = @At("HEAD"), cancellable = true)
    private void doore$showModel(CallbackInfoReturnable<Boolean> cir) {
        if (SmoothF5Feature.transitioning()) {
            cir.setReturnValue(true);
        }
    }
}
