package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.overlay.HudFeature;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public final class PickupLoggerFeature extends Feature implements Tick {
    private final Opt.Flag donateOnly = this.opt(new Opt.Flag("donate_only", "Только донат", false));
    private final Opt.Flag island = this.opt(new Opt.Flag("island", "В Island", true));
    private Map<String, Integer> last = Map.of();

    public PickupLoggerFeature() {
        super("pickup_logger", "Pickup Logger", "Тост при подборе предмета", Category.TOOLS, false);
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on() && mc.player != null) {
            Map<String, Integer> map = this.snap(mc.player.getInventory());
            if (!this.last.isEmpty()) {
                for (Entry<String, Integer> entry : map.entrySet()) {
                    int i = entry.getValue() - this.last.getOrDefault(entry.getKey(), 0);
                    if (i > 0) {
                        ItemStack itemstack = this.sample(mc.player.getInventory(), entry.getKey());
                        if (!(Boolean)this.donateOnly.get() || AuctionLore.donate(itemstack)) {
                            String s = AuctionLore.displayName(itemstack);
                            if (s.isBlank()) {
                                s = entry.getKey();
                            }

                            String s1 = "+" + i + "  " + s;
                            App.features().find(HudFeature.class).ifPresent(h -> {
                                if ((Boolean)this.island.get()) {
                                    h.islandEvent("Подбор", s1);
                                } else {
                                    h.notify("Подбор", s1, HudFeature.NoteKind.OK);
                                }
                            });
                        }
                    }
                }
            }

            this.last = map;
        } else {
            this.last = Map.of();
        }
    }

    private Map<String, Integer> snap(PlayerInventory inv) {
        Map<String, Integer> map = new HashMap<>();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack itemstack = inv.getStack(i);
            if (!itemstack.isEmpty()) {
                map.merge(key(itemstack), itemstack.getCount(), Integer::sum);
            }
        }

        return map;
    }

    private ItemStack sample(PlayerInventory inv, String key) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack itemstack = inv.getStack(i);
            if (key.equals(key(itemstack))) {
                return itemstack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static String key(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()) + "|" + AuctionLore.displayName(stack);
    }
}
