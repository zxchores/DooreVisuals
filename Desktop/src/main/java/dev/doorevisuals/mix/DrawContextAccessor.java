package dev.doorevisuals.mix;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The GUI render state is where every drawn element is queued, and reaching it is the only way to
 * submit a render state of our own instead of going through one of the fixed {@code DrawContext}
 * helpers.
 */
@Mixin(DrawContext.class)
public interface DrawContextAccessor {
    @Accessor("state")
    GuiRenderState doore$state();
}
