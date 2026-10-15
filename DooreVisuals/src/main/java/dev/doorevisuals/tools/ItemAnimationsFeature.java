package dev.doorevisuals.tools;

import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;

/**
 * Item Animations: how fast and how nicely the item in the hand is swapped, and how eating and drinking look. The values are
 * read live by ItemAnimMixin, so a change is visible at once.
 */
public final class ItemAnimationsFeature extends Feature {
    public static final String EAT_VANILLA = "Vanilla";
    public static final String EAT_SOFT = "Мягкая";
    public static final String EAT_STRONG = "Сильная";
    public static final String EAT_CENTER = "Центр";
    public static final String EAT_NONE = "Без анимации";
    public static final String SWAP_NONE = "Нет";
    public static final String SWAP_TILT = "Наклон";
    public static final String SWAP_ROLL = "Вираж";

    private static ItemAnimationsFeature self;

    private final Opt.Num swapSpeed = this.opt(new Opt.Num("swap_speed", "Скорость смены предмета", 1.6, 1.0, 3.0, 0.1));
    private final Opt.Pick swapStyle = this.opt(new Opt.Pick("swap_style", "Переход при смене", SWAP_TILT, SWAP_NONE, SWAP_TILT, SWAP_ROLL).chips());
    private final Opt.Pick eatStyle = this.opt(new Opt.Pick("eat_style", "Еда и питьё", EAT_SOFT, EAT_VANILLA, EAT_SOFT, EAT_STRONG, EAT_CENTER, EAT_NONE).chips());
    private final Opt.Num eatPower = this.opt(new Opt.Num("eat_power", "Сила анимации еды", 1.0, 0.3, 2.0, 0.1).visibleWhen(
            () -> !EAT_VANILLA.equals(this.eatStyle.get()) && !EAT_NONE.equals(this.eatStyle.get())));

    public ItemAnimationsFeature() {
        super(
                "item_animations",
                "Item Animations",
                "Быстрая плавная смена предмета в руке и свои анимации еды и питья",
                Category.TOOLS,
                true
        );
        this.badge(Badge.NEW);
        self = this;
    }

    /** How much faster than the game the swap goes: 0 when the module is off, otherwise the share of the way covered extra per tick. */
    public static float swapNudge() {
        ItemAnimationsFeature f = self;
        return f == null || !f.on() ? 0.0F : Math.min(0.7F, (f.swapSpeed.f() - 1.0F) * 0.3F);
    }

    /** The transition while the item is lowered or raised: it tilts or rolls, so the swap reads as one smooth movement. */
    public static void swapPose(net.minecraft.client.util.math.MatrixStack pose, net.minecraft.util.Arm arm, float equipProgress) {
        String style = swapStyle();
        if (equipProgress <= 0.001F || SWAP_NONE.equals(style)) {
            return;
        }
        int side = arm == net.minecraft.util.Arm.RIGHT ? 1 : -1;
        float e = equipProgress * equipProgress;
        if (SWAP_ROLL.equals(style)) {
            pose.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(side * e * -34.0F));
            pose.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(side * e * 18.0F));
        } else {
            pose.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(e * 32.0F));
            pose.translate(0.0F, -0.04F * e, 0.0F);
        }
    }

    public static String swapStyle() {
        ItemAnimationsFeature f = self;
        return f == null || !f.on() ? SWAP_NONE : f.swapStyle.get();
    }

    /** The eating style, or Vanilla when the module is off. */
    public static String eatStyle() {
        ItemAnimationsFeature f = self;
        return f == null || !f.on() ? EAT_VANILLA : f.eatStyle.get();
    }

    public static float eatPower() {
        ItemAnimationsFeature f = self;
        return f == null ? 1.0F : f.eatPower.f();
    }
}
