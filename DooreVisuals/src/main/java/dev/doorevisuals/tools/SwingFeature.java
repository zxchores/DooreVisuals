package dev.doorevisuals.tools;

import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.MathHelper;

/**
 * Swing Animations: replaces the first-person swing of the hand with one of eleven own animations (the poses are in
 * {@link SwingAnimations}). Strength scales the angles and the reach, speed shortens or lengthens the swing itself.
 * The values are read again whenever an option changes, so nothing needs a restart.
 */
public final class SwingFeature extends Feature {
    private static final float BASE_SPEED = 6.0F;

    private static SwingFeature self;

    private final Opt.Pick typeOpt = this.opt(
            new Opt.Pick("type", "Type", "Mod 1", SwingAnimations.TYPES.toArray(new String[0])).chips());
    private final Opt.Num strengthOpt = this.opt(new Opt.Num("strength", "Swing Strength", 3.0, 0.0, 6.0, 0.1));
    private final Opt.Num speedOpt = this.opt(new Opt.Num("speed", "Swing Speed", 6.0, 1.0, 10.0, 0.5));
    private final Opt.Flag smoothOpt = this.opt(new Opt.Flag("smooth", "Smooth Animation", true));
    private final Opt.Flag swordsOpt = this.opt(new Opt.Flag("only_swords", "Only Swords", false));

    public SwingFeature() {
        super(
                "swing",
                "Swing Animations",
                "Свои анимации взмаха от первого лица: 11 режимов, сила и скорость",
                Category.TOOLS,
                true
        );
        this.badge(Badge.NEW);
        self = this;
    }

    // The values are read from the options on every call, so a change in the menu is visible on the next frame.

    /** True while the module is on and a custom type is chosen; with Vanilla the game's own swing is untouched. */
    public static boolean active() {
        SwingFeature f = self;
        return f != null && f.on() && SwingAnimations.custom(f.typeOpt.get());
    }

    public static String type() {
        SwingFeature f = self;
        return f == null ? SwingAnimations.VANILLA : f.typeOpt.get();
    }

    public static float strength() {
        SwingFeature f = self;
        return f == null ? 0.0F : MathHelper.clamp(f.strengthOpt.f(), 0.0F, 6.0F);
    }

    public static boolean onlySwords() {
        SwingFeature f = self;
        return f != null && f.swordsOpt.get();
    }

    /** Whether the swing of the hand holding this stack is replaced (an empty hand is not a sword). */
    public static boolean appliesTo(ItemStack stack) {
        if (!active()) {
            return false;
        }
        return !onlySwords() || (stack != null && !stack.isEmpty() && stack.isIn(ItemTags.SWORDS));
    }

    /** The progress handed to the pose: the game's linear progress, eased a little when Smooth Animation is on. */
    public static float shape(float progress) {
        float p = MathHelper.clamp(progress, 0.0F, 1.0F);
        SwingFeature f = self;
        if (f == null || !f.smoothOpt.get()) {
            return p;
        }
        float eased = p * p * p * (p * (p * 6.0F - 15.0F) + 10.0F);
        return p + (eased - p) * 0.6F;
    }

    /**
     * How much longer or shorter the swing lasts than in the game: 1 at the default speed, below 1 when faster. A
     * soft power law, so the slowest setting is about four times slower and the fastest about 1.5 times quicker.
     */
    public static float durationScale() {
        SwingFeature f = self;
        if (!active() || f == null) {
            return 1.0F;
        }
        float speed = MathHelper.clamp(f.speedOpt.f(), 1.0F, 10.0F);
        return (float)Math.pow(BASE_SPEED / speed, 0.75);
    }
}
