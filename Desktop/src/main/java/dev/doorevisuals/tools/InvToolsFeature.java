package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class InvToolsFeature extends Feature {
    private final Opt.Flag throwArmor = this.opt(
        new Opt.Flag("throw_armor", "\u0412\u044b\u0431\u0440\u0430\u0441\u044b\u0432\u0430\u0442\u044c \u0431\u0440\u043e\u043d\u044e", false)
    );
    private final Opt.Flag throwCraft = this.opt(
        new Opt.Flag("throw_craft", "\u0412\u044b\u0431\u0440\u0430\u0441\u044b\u0432\u0430\u0442\u044c \u043a\u0440\u0430\u0444\u0442", false)
    );

    public InvToolsFeature() {
        super(
            "inv_tools",
            "\u0418\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c",
            "\u0412\u044b\u0431\u0440\u043e\u0441 \u0438 \u0441\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u043a\u0430 \u0432 \u0441\u0443\u043d\u0434\u0443\u043a\u0430\u0445",
            Category.TOOLS,
            false
        );
    }

    public boolean includeArmor() {
        return (Boolean)this.throwArmor.get();
    }

    public boolean includeCraft() {
        return (Boolean)this.throwCraft.get();
    }

    public static void dump(MinecraftClient mc) {
        InvToolsFeature invtoolsfeature = live();
        if (invtoolsfeature != null && mc.player != null && mc.currentScreen instanceof HandledScreen<?> handledscreen) {
            ScreenHandler screenhandler = handledscreen.getScreenHandler();
            int id = screenhandler.syncId;

            for (Slot slot : screenhandler.slots) {
                if (invtoolsfeature.dumpable(slot, mc.player) && !slot.getStack().isEmpty()) {
                    InvClicks.throwAll(id, slot.id);
                }
            }
        }
    }

    public static void sort(MinecraftClient mc) {
        InvToolsFeature invtoolsfeature = live();
        if (invtoolsfeature != null && mc.player != null && mc.currentScreen instanceof HandledScreen<?> handledscreen) {
            ScreenHandler screenhandler = handledscreen.getScreenHandler();
            int id = screenhandler.syncId;
            ArrayList<Slot> slots = new ArrayList<>();

            for (Slot slot : screenhandler.slots) {
                if (invtoolsfeature.dumpable(slot, mc.player)) {
                    slots.add(slot);
                }
            }

            slots.sort(Comparator.comparingInt(s -> s.id));
            List<ItemStack> list = new ArrayList<>();

            for (Slot slot1 : slots) {
                list.add(slot1.getStack().copy());
            }

            list.sort(InvToolsFeature::compareStacks);

            for (int i1 = 0; i1 < slots.size(); i1++) {
                ItemStack itemstack = list.get(i1);
                int j = ((Slot)slots.get(i1)).id;
                if (!same(((Slot)slots.get(i1)).getStack(), itemstack)) {
                    int k = -1;

                    for (int l = i1 + 1; l < slots.size(); l++) {
                        if (same(((Slot)slots.get(l)).getStack(), itemstack)) {
                            k = ((Slot)slots.get(l)).id;
                            break;
                        }
                    }

                    if (k >= 0) {
                        InvClicks.swap(id, j, k);
                    }
                }
            }
        }
    }

    private boolean dumpable(Slot slot, PlayerEntity player) {
        if (slot != null && slot.canTakeItems(player)) {
            if (slot.inventory instanceof PlayerInventory playerinventory && playerinventory.player == player) {
                int i = slot.getIndex();
                if (!(Boolean)this.throwArmor.get() && i >= 36 && i <= 39) {
                    return false;
                } else {
                    return !this.throwCraft.get() && i < 0 ? false : i >= 0 && i <= 40;
                }
            } else {
                if (!slot.hasStack()) {
                }

                return true;
            }
        } else {
            return false;
        }
    }

    private static int compareStacks(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0;
        } else if (a.isEmpty()) {
            return 1;
        } else if (b.isEmpty()) {
            return -1;
        } else {
            int i = Registries.ITEM.getId(a.getItem()).compareTo(Registries.ITEM.getId(b.getItem()));
            return i != 0 ? i : Integer.compare(b.getCount(), a.getCount());
        }
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return a.isEmpty() && b.isEmpty() ? true : ItemStack.areItemsAndComponentsEqual(a, b) && a.getCount() == b.getCount();
    }

    private static InvToolsFeature live() {
        return !App.live() ? null : App.features().find(InvToolsFeature.class).filter(Feature::on).orElse(null);
    }
}
