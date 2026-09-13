package dev.doorevisuals.mix;

import dev.doorevisuals.world.AtmosphereFeature;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public class CloudMixin {
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void doore$clouds(int color, CloudRenderMode cloudsType, float cloudHeight, Vec3d cameraPos, long time, float ticks, CallbackInfo ci) {
        if (AtmosphereFeature.hideVanillaClouds()) {
            ci.cancel();
        }
    }
}
