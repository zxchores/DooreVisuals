package dev.doorevisuals.mix;

import dev.doorevisuals.world.AtmosphereFeature;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class SkyMixin {
    @Inject(method = "renderTopSky", at = @At("HEAD"), cancellable = true)
    private void doore$skyDisc(int color, CallbackInfo ci) {
        if (AtmosphereFeature.hideVanillaSky()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSkyDark", at = @At("HEAD"), cancellable = true)
    private void doore$darkDisc(CallbackInfo ci) {
        if (AtmosphereFeature.hideVanillaSky()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCelestialBodies", at = @At("HEAD"), cancellable = true)
    private void doore$celestial(
        MatrixStack pose, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float alpha, float starBrightness, CallbackInfo ci
    ) {
        if (AtmosphereFeature.hideVanillaCelestial()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderGlowingSky", at = @At("HEAD"), cancellable = true)
    private void doore$sunrise(MatrixStack pose, float solarAngle, int color, CallbackInfo ci) {
        if (AtmosphereFeature.hideVanillaSky()) {
            ci.cancel();
        }
    }
}
