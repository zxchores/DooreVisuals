package dev.doorevisuals.mix;

import dev.doorevisuals.tools.HandShaderFeature;
import java.util.List;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Hand Shader on the item in the hand: right before the game pops the item's transform, the same quads are submitted
 * once more with the wave layer, so the waves lie exactly on the item and move with it.
 */
@Mixin(ItemRenderState.LayerRenderState.class)
public abstract class ItemWaveMixin {
    @Shadow
    private RenderLayer renderLayer;
    @Shadow
    private int[] tints;

    @Shadow
    public abstract List<BakedQuad> getQuads();

    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", shift = At.Shift.BEFORE)
    )
    private void doore$waves(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, int outline, CallbackInfo ci) {
        if (this.renderLayer != null) {
            HandShaderFeature.submitItemWaves(matrices, queue, this.getQuads(), this.tints, outline);
        }
    }
}
