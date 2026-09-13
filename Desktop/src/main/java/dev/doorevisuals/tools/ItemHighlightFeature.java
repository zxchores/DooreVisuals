package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Theme;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class ItemHighlightFeature extends Feature {
    private static volatile boolean live;
    private static volatile boolean gapples = true;
    private static volatile boolean potions = true;
    private static volatile boolean totems = true;
    private static volatile boolean pearls = true;
    private static volatile boolean chorus = true;
    private static volatile boolean netherite = true;
    private final Opt.Flag gapple = this.opt(new Opt.Flag("gapple", "\u0413\u0435\u043f\u043b\u044b", true));
    private final Opt.Flag potion = this.opt(new Opt.Flag("potion", "\u0427\u0430\u0440\u043a\u0438 / \u0437\u0435\u043b\u044c\u044f", true));
    private final Opt.Flag totem = this.opt(new Opt.Flag("totem", "\u0422\u043e\u0442\u0435\u043c", true));
    private final Opt.Flag pearl = this.opt(new Opt.Flag("pearl", "\u041f\u0435\u0440\u043b\u044b", true));
    private final Opt.Flag chorusFruit = this.opt(new Opt.Flag("chorus", "\u0425\u043e\u0440\u0443\u0441\u044b", true));
    private final Opt.Flag netheriteArmor = this.opt(
        new Opt.Flag("netherite", "\u041d\u0435\u0437\u0435\u0440\u0438\u0442\u043e\u0432\u0430\u044f \u0431\u0440\u043e\u043d\u044f", true)
    );

    public ItemHighlightFeature() {
        super(
            "item_highlight",
            "Item Highlight",
            "\u041f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0430 \u0433\u0435\u043f\u043b\u043e\u0432, \u0447\u0430\u0440\u043e\u043a, \u0442\u043e\u0442\u0435\u043c\u0430, \u043f\u0435\u0440\u043b\u043e\u0432, \u0445\u043e\u0440\u0443\u0441\u043e\u0432 \u0438 \u043d\u0435\u0437\u0435\u0440\u0438\u0442\u0430 \u0432 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u0435",
            Category.OVERLAY,
            true
        );
    }

    public static boolean active() {
        return live;
    }

    public static boolean matches(ItemStack stack) {
        if (live && stack != null && !stack.isEmpty()) {
            Item item = stack.getItem();
            if (!gapples || item != Items.GOLDEN_APPLE && item != Items.ENCHANTED_GOLDEN_APPLE) {
                if (!potions || item != Items.POTION && item != Items.SPLASH_POTION && item != Items.LINGERING_POTION && item != Items.TIPPED_ARROW) {
                    if (totems && item == Items.TOTEM_OF_UNDYING) {
                        return true;
                    } else if (pearls && item == Items.ENDER_PEARL) {
                        return true;
                    } else {
                        return chorus && item == Items.CHORUS_FRUIT
                            ? true
                            : netherite && (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS);
                    }
                } else {
                    return true;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public static int glowColor() {
        return Theme.alpha(ThemeFeature.hudTint(), 90);
    }

    public static int rimColor() {
        return Theme.alpha(Theme.ACCENT_HOT, 180);
    }

    @Override
    protected void enable() {
        this.sync();
    }

    @Override
    protected void disable() {
        live = false;
    }

    @Override
    public void poke() {
        super.poke();
        this.sync();
    }

    private void sync() {
        live = this.on();
        gapples = (Boolean)this.gapple.get();
        potions = (Boolean)this.potion.get();
        totems = (Boolean)this.totem.get();
        pearls = (Boolean)this.pearl.get();
        chorus = (Boolean)this.chorusFruit.get();
        netherite = (Boolean)this.netheriteArmor.get();
    }
}
