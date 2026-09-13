package dev.doorevisuals.mix;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.doorevisuals.draw.GlintColorState;
import dev.doorevisuals.tools.ThemeFeature;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemRenderer.class)
public class GlintColorMixin {
    @ModifyReturnValue(method = "getTint", at = @At("RETURN"))
    private static int doore$tint(int original) {
        return !GlintColorState.active() ? original : ColorHelper.mix(original, ThemeFeature.effectTint() | 0xFF000000);
    }
}
