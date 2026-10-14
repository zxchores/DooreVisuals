package dev.doorevisuals.mix;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;

/** Dynamic Island input is intentionally handled only by ChatHudMixin while ChatScreen is open. */
@Mixin(Mouse.class)
public abstract class IslandMouseMixin {
    @SuppressWarnings("unused")
    private static void doore$disabledOutsideChat() {
        // Intentionally empty.
    }
}
