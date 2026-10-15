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
            true
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

    /** Sorts what the open screen is about: the player's inventory in the inventory screen, the container in a chest, barrel or shulker box. */
    public static void sort(MinecraftClient mc) {
        sort(mc, mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen);
    }

    /**
     * Sorts one region of the open screen without moving anything between the container and the inventory: the container
     * (chest, ender chest, barrel, shulker box) or the main 27 slots of the player's inventory (the hotbar, armor and the
     * crafting grid stay as they are). Equal items are merged first, then the stacks are ordered by item and by size and
     * moved into place; the clicks go through the click queue, a few ticks apart, so a server is not flooded.
     */
    public static void sort(MinecraftClient mc, boolean playerRegion) {
        InvToolsFeature feature = live();
        if (feature == null || mc.player == null || !(mc.currentScreen instanceof HandledScreen<?> screen) || InvClicks.busy()) {
            return;
        }
        ScreenHandler handler = screen.getScreenHandler();
        List<Slot> region = new ArrayList<>();
        for (Slot slot : handler.slots) {
            boolean mine = slot.inventory instanceof PlayerInventory inv && inv.player == mc.player;
            if (playerRegion) {
                if (mine && slot.getIndex() >= 9 && slot.getIndex() <= 35) {
                    region.add(slot);
                }
            } else if (!mine && (handler instanceof net.minecraft.screen.GenericContainerScreenHandler || handler instanceof net.minecraft.screen.ShulkerBoxScreenHandler)
                    && slot.canTakeItems(mc.player)) {
                region.add(slot);
            }
        }
        if (region.size() < 2) {
            return;
        }
        region.sort(Comparator.comparingInt(sl -> sl.id));
        int n = region.size();
        ItemStack[] cur = new ItemStack[n];
        for (int i = 0; i < n; i++) {
            cur[i] = region.get(i).getStack().copy();
        }
        int menu = handler.syncId;

        // 1. merge partial stacks of the same item: pick up the later one, drop it on the earlier one, put the rest back
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n && !cur[i].isEmpty() && cur[i].getCount() < cur[i].getMaxCount(); j++) {
                if (!cur[j].isEmpty() && ItemStack.areItemsAndComponentsEqual(cur[i], cur[j])) {
                    int move = Math.min(cur[i].getMaxCount() - cur[i].getCount(), cur[j].getCount());
                    InvClicks.pickup(menu, region.get(j).id);
                    InvClicks.pickup(menu, region.get(i).id);
                    cur[i].increment(move);
                    cur[j].decrement(move);
                    if (!cur[j].isEmpty()) {
                        InvClicks.pickup(menu, region.get(j).id);
                    }
                }
            }
        }

        // 2. the wanted order: by item id, bigger stacks first, empty slots at the end
        List<ItemStack> wanted = new ArrayList<>();
        for (ItemStack st : cur) {
            if (!st.isEmpty()) {
                wanted.add(st.copy());
            }
        }
        wanted.sort(InvToolsFeature::compareStacks);

        // 3. put every stack in its place with a swap
        for (int p = 0; p < wanted.size(); p++) {
            ItemStack want = wanted.get(p);
            if (same(cur[p], want)) {
                continue;
            }
            int q = -1;
            for (int k = p + 1; k < n; k++) {
                if (same(cur[k], want)) {
                    q = k;
                    break;
                }
            }
            if (q >= 0) {
                InvClicks.swap(menu, region.get(p).id, region.get(q).id);
                ItemStack t = cur[p];
                cur[p] = cur[q];
                cur[q] = t;
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
