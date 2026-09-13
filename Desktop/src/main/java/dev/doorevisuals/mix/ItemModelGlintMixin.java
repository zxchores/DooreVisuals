package dev.doorevisuals.mix;

import dev.doorevisuals.draw.GlintColorState;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelManager.class)
public class ItemModelGlintMixin {
    @Inject(method = "clearAndUpdate", at = @At("HEAD"))
    private void doore$enter(ItemRenderState state, ItemStack stack, ItemDisplayContext ctx, World level, HeldItemContext owner, int seed, CallbackInfo ci) {
        GlintColorState.enter(stack);
    }

    @Inject(method = "clearAndUpdate", at = @At("RETURN"))
    private void doore$exit(ItemRenderState state, ItemStack stack, ItemDisplayContext ctx, World level, HeldItemContext owner, int seed, CallbackInfo ci) {
        GlintColorState.exit();
    }
}
