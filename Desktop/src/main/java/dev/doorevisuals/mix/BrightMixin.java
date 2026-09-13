package dev.doorevisuals.mix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.Std140Builder;
import dev.doorevisuals.tools.BrightFeature;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public abstract class BrightMixin {
    @Shadow
    private boolean dirty;
    @Unique
    private int doore$floatIdx;

    @Inject(method = "tick", at = @At("TAIL"))
    private void doore$keepDirty(CallbackInfo ci) {
        if (BrightFeature.active()) {
            this.dirty = true;
        }
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void doore$resetIdx(float partialTick, CallbackInfo ci) {
        this.doore$floatIdx = 0;
    }

    @WrapOperation(
        method = "update",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;")
    )
    private Std140Builder doore$boostLightmap(Std140Builder builder, float value, Operation<Std140Builder> original) {
        int i = this.doore$floatIdx++;
        if (BrightFeature.active()) {
            float f = BrightFeature.power();

            value = switch (i) {
                case 3 -> f;
                case 4 -> 0.0F;
                case 5 -> 0.0F;
                case 6 -> Math.max(value, f);
                default -> value;
            };
        }

        return (Std140Builder)original.call(new Object[]{builder, value});
    }
}
