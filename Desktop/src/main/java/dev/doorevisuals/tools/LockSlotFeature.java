package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.SlotActionType;

public final class LockSlotFeature extends Feature {
    private final Opt.Text mask = this.opt(new Opt.Text("mask", "Слоты 0-8", "0 1 2"));
    private final Opt.Flag offhand = this.opt(new Opt.Flag("offhand", "Оффхенд", true));

    public LockSlotFeature() {
        super("lock_slot", "Lock Slot", "Блок Q и выброса с хотбара. Серверный кик не отменяем", Category.TOOLS, false);
    }

    public static boolean blockDrop() {
        LockSlotFeature lockslotfeature = live();
        if (lockslotfeature == null) {
            return false;
        } else {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            return minecraftclient.player != null && lockslotfeature.hotbar(minecraftclient.player.getInventory().getSelectedSlot());
        }
    }

    public static boolean blockClick(int slotId, SlotActionType type) {
        LockSlotFeature lockslotfeature = live();
        if (lockslotfeature != null && type == SlotActionType.THROW) {
            if (slotId >= 36 && slotId <= 44) {
                return lockslotfeature.hotbar(slotId - 36);
            } else {
                return slotId == 45 && (Boolean)lockslotfeature.offhand.get();
            }
        } else {
            return false;
        }
    }

    private boolean hotbar(int slot) {
        if (slot < 0 || slot > 8) {
            return false;
        } else {
            String s = this.mask.get() == null ? "" : (String)this.mask.get();

            for (String s1 : s.split("[,;\\s]+")) {
                if (s1.equals(String.valueOf(slot))) {
                    return true;
                }
            }

            return false;
        }
    }

    private static LockSlotFeature live() {
        return App.features().find(LockSlotFeature.class).filter(Feature::on).orElse(null);
    }
}
