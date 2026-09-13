package dev.doorevisuals.mix;

import dev.doorevisuals.tools.NoRenderFeature;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlockState.class)
public class GrassFxMixin {
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void doore$grass(CallbackInfoReturnable<BlockRenderType> cir) {
        if (NoRenderFeature.grass()) {
            BlockState blockstate = (BlockState)this;
            if (NoRenderFeature.isGrass(blockstate)) {
                cir.setReturnValue(BlockRenderType.INVISIBLE);
            }
        }
    }
}
