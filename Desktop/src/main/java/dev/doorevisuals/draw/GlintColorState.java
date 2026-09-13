package dev.doorevisuals.draw;

import dev.doorevisuals.tools.ThemeFeature;
import net.minecraft.item.ItemStack;

public final class GlintColorState {
    private static final ThreadLocal<Boolean> FOIL = ThreadLocal.withInitial(() -> false);

    private GlintColorState() {
    }

    public static void enter(ItemStack stack) {
        FOIL.set(stack != null && !stack.isEmpty() && stack.hasGlint() && ThemeFeature.themeGlint());
    }

    public static void mark(boolean foil) {
        FOIL.set(foil && ThemeFeature.themeGlint());
    }

    public static void exit() {
        FOIL.set(false);
    }

    public static boolean active() {
        return Boolean.TRUE.equals(FOIL.get());
    }
}
