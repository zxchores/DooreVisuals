package dev.doorevisuals.mix;

import dev.doorevisuals.tools.NoRenderFeature;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherRendering.class)
public class WeatherFxMixin {
    @Inject(method = "renderPrecipitation", at = @At("HEAD"), cancellable = true)
    private void doore$weather(VertexConsumerProvider buffers, Vec3d cam, WeatherRenderState state, CallbackInfo ci) {
        if (NoRenderFeature.weather()) {
            ci.cancel();
        }
    }
}
