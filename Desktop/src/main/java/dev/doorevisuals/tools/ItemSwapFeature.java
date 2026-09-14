package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

public final class ItemSwapFeature extends Feature implements Tick {
    private final Opt.Pick mode = this.opt(new Opt.Pick("mode", "Режим", "Двойной", "Двойной", "Тройной"));
    private final Opt.Flag offhand = this.opt(new Opt.Flag("offhand", "В оффхенд", false));
    private final Opt.Pick preset = this.opt(
        new Opt.Pick("preset", "Пресет", "Сфера / талисман", "Сфера / талисман", "Меч", "Еда", "Свой")
    );
    private final Opt.Text filters = this.opt(new Opt.Text("filters", "Фильтры", "Сфера, Талисман"));
    private final Opt.Key swap = this.opt(new Opt.Key("swap", "Свап", -1));
    private boolean swapWasDown;
    private int cursor;

    public ItemSwapFeature() {
        super("item_swap", "Item Swap", "Цикл 2/3 предметов по имени/лору в хотбаре без открытия инвентаря", Category.TOOLS, false);
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (mc.player != null && mc.currentScreen == null) {
            int i = (Integer)this.swap.get();
            if (i > 0 && i != -1) {
                boolean flag = GLFW.glfwGetKey(mc.getWindow().getHandle(), i) == 1;
                if (flag && !this.swapWasDown && !InvClicks.busy()) {
                    this.swapNext(mc);
                }

                this.swapWasDown = flag;
            } else {
                this.swapWasDown = false;
            }
        } else {
            this.swapWasDown = false;
        }
    }

    private void swapNext(MinecraftClient mc) {
        PlayerScreenHandler playerscreenhandler = mc.player.playerScreenHandler;
        int i = this.offhand.get() ? 45 : 36 + mc.player.getInventory().getSelectedSlot();
        List<Integer> list = this.findMatches(playerscreenhandler);
        if (!list.isEmpty()) {
            int j = Math.min(list.size(), "Тройной".equals(this.mode.get()) ? 3 : 2);
            List<Integer> list1 = list.subList(0, j);
            this.cursor = this.cursor % list1.size();
            int k = list1.get(this.cursor);
            this.cursor = (this.cursor + 1) % list1.size();
            if (k == i) {
                k = list1.get(this.cursor);
                this.cursor = (this.cursor + 1) % list1.size();
            }

            InvClicks.swap(0, k, i);
        }
    }

    private List<Integer> findMatches(PlayerScreenHandler menu) {
        List<String> list = this.parseFilters();
        List<Integer> list1 = new ArrayList<>();

        for (int i = 9; i <= 45 && i < menu.slots.size(); i++) {
            ItemStack itemstack = ((Slot)menu.slots.get(i)).getStack();
            if (!itemstack.isEmpty() && matches(itemstack, list)) {
                list1.add(i);
            }
        }

        return list1;
    }

    private List<String> parseFilters() {
        String s = switch ((String)this.preset.get()) {
            case "Меч" -> "меч, sword, клинок";
            case "Еда" -> "яблок, еда, apple, хлеб, стейк";
            case "Свой" -> this.filters.get() == null ? "" : (String)this.filters.get();
            default -> "сфера, талисман";
        };
        List<String> list = new ArrayList<>();

        for (String s1 : s.split("[,;\\n]")) {
            String s2 = s1.trim().toLowerCase(Locale.ROOT);
            if (!s2.isEmpty()) {
                list.add(s2);
            }
        }

        if (list.isEmpty()) {
            list.add("сфера");
            list.add("талисман");
        }

        return list;
    }

    private static boolean matches(ItemStack st, List<String> keys) {
        String s = AuctionLore.blob(st).toLowerCase(Locale.ROOT);

        for (String s1 : keys) {
            if (s.contains(s1)) {
                return true;
            }
        }

        return false;
    }
}
