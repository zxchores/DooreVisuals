package dev.doorevisuals.mix;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doorevisuals.draw.GlintColorState;
import dev.doorevisuals.tools.SwordFeature;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemModelManager.class)
public class ItemModelGlintMixin {
    /**
     * Wraps the whole call. A chosen custom sword is drawn from a copy of the stack that carries the custom item model,
     * and the real stack is left alone, so the inventory and the server never see the change. The glint colour is set
     * for the duration of the call and always restored.
     */
    @WrapMethod(method = "clearAndUpdate")
    private void doore$clearAndUpdate(
            ItemRenderState state,
            ItemStack stack,
            ItemDisplayContext ctx,
            World level,
            HeldItemContext owner,
            int seed,
            Operation<Void> original
    ) {
        GlintColorState.enter(stack);
        try {
            original.call(state, SwordFeature.visualStack(stack, ctx), ctx, level, owner, seed);
        } finally {
            GlintColorState.exit();
        }
    }
}
