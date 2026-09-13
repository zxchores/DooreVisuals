package dev.doorevisuals.mix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doorevisuals.tools.NoRenderFeature;
import dev.doorevisuals.world.AmbienceFeature;
import dev.doorevisuals.world.FogFeature;
import java.nio.ByteBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.render.fog.LavaFogModifier;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private void doore$fogColor(Camera camera, float partialTick, ClientWorld level, int renderDistance, float bossTint, CallbackInfoReturnable<Vector4f> cir) {
        if (FogFeature.active()) {
            Vector4f vector4f = new Vector4f((Vector4fc)cir.getReturnValue());
            AmbienceFeature.applyColor(vector4f);
            cir.setReturnValue(vector4f);
        }
    }

    @WrapOperation(
        method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V")
    )
    private void doore$fogBuffer(
        FogRenderer self,
        ByteBuffer buffer,
        int bufPos,
        Vector4f color,
        float environmentalStart,
        float environmentalEnd,
        float renderDistanceStart,
        float renderDistanceEnd,
        float skyEnd,
        float cloudEnd,
        Operation<Void> original
    ) {
        if (FogFeature.active()) {
            AmbienceFeature.applyColor(color);
            float[] afloat = FogFeature.bufferDistances(renderDistanceEnd);
            original.call(new Object[]{self, buffer, bufPos, color, afloat[0], afloat[1], afloat[2], afloat[3], afloat[4], afloat[5]});
        } else {
            original.call(
                new Object[]{self, buffer, bufPos, color, environmentalStart, environmentalEnd, renderDistanceStart, renderDistanceEnd, skyEnd, cloudEnd}
            );
        }
    }

    @Mixin(AtmosphericFogModifier.class)
    public static class Atmosphere {
        @Inject(method = "applyStartEndModifier", at = @At("RETURN"))
        private void doore$atmo(FogData fogData, Camera camera, ClientWorld level, float renderDistanceBlocks, RenderTickCounter delta, CallbackInfo ci) {
            if (FogFeature.active()) {
                FogFeature.applyDistance(fogData, renderDistanceBlocks);
            }
        }
    }

    @Mixin(LavaFogModifier.class)
    public static class Lava {
        @Inject(method = "applyStartEndModifier", at = @At("HEAD"), cancellable = true)
        private void doore$lava(FogData fogData, Camera camera, ClientWorld level, float renderDistanceBlocks, RenderTickCounter delta, CallbackInfo ci) {
            if (NoRenderFeature.fire()) {
                fogData.environmentalStart = 0.0F;
                fogData.environmentalEnd = 0.01F;
                ci.cancel();
            }
        }
    }
}
