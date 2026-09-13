package dev.doorevisuals.mix;

import dev.doorevisuals.tools.ItemHighlightFeature;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class ItemHighlightMixin {
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void doore$glow(DrawContext g, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (ItemHighlightFeature.active() && slot != null) {
            ItemStack itemstack = slot.getStack();
            if (ItemHighlightFeature.matches(itemstack)) {
                int i = slot.x;
                int j = slot.y;
                g.fill(i - 1, j - 1, i + 17, j + 17, ItemHighlightFeature.glowColor());
                int k = ItemHighlightFeature.rimColor();
                g.fill(i - 1, j - 1, i + 17, j, k);
                g.fill(i - 1, j + 16, i + 17, j + 17, k);
                g.fill(i - 1, j, i, j + 16, k);
                g.fill(i + 16, j, i + 17, j + 16, k);
            }
        }
    }
}
