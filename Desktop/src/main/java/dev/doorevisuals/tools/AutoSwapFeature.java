package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class AutoSwapFeature extends Feature implements Tick {
    private final Opt.Pick mode = this.opt(new Opt.Pick("mode", "\u0420\u0435\u0436\u0438\u043c", "Double", "Double", "Triple"));
    private final Opt.Flag offhand = this.opt(new Opt.Flag("offhand", "\u0412 \u043e\u0444\u0444\u0445\u044d\u043d\u0434", false));
    private final Opt.Text filters = this.opt(
        new Opt.Text(
            "filters", "\u0424\u0438\u043b\u044c\u0442\u0440\u044b", "\u0421\u0444\u0435\u0440\u0430, \u0422\u0430\u043b\u0438\u0441\u043c\u0430\u043d"
        )
    );
    private final Opt.Key swap = this.opt(new Opt.Key("swap", "\u0421\u0432\u0430\u043f", -1));
    private boolean swapWasDown;
    private int cursor;

    public AutoSwapFeature() {
        super(
            "auto_swap",
            "AutoSwap",
            "\u0426\u0438\u043a\u043b \u043f\u0440\u0435\u0434\u043c\u0435\u0442\u043e\u0432 \u043f\u043e \u0438\u043c\u0435\u043d\u0438/\u043b\u043e\u0440\u0443 \u0432 \u0445\u043e\u0442\u0431\u0430\u0440. Funtime / Spookytime / HollyWorld / Funsky / Spacetimes",
            Category.TOOLS,
            false
        );
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
        List<Integer> list = this.findMatches(playerscreenhandler, mc.player.getInventory());
        if (!list.isEmpty()) {
            int j = Math.min(list.size(), "Triple".equals(this.mode.get()) ? 3 : 2);
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

    private List<Integer> findMatches(PlayerScreenHandler menu, PlayerInventory inv) {
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
        List<String> list = new ArrayList<>();
        String s = this.filters.get() == null ? "" : (String)this.filters.get();

        for (String s1 : s.split("[,;\\n]")) {
            String s2 = s1.trim().toLowerCase(Locale.ROOT);
            if (!s2.isEmpty()) {
                list.add(s2);
            }
        }

        if (list.isEmpty()) {
            list.add("\u0441\u0444\u0435\u0440\u0430");
            list.add("\u0442\u0430\u043b\u0438\u0441\u043c\u0430\u043d");
        }

        return list;
    }

    private static boolean matches(ItemStack st, List<String> keys) {
        String s = st.getName().getString().toLowerCase(Locale.ROOT);
        StringBuilder stringbuilder = new StringBuilder(s);
        LoreComponent lorecomponent = (LoreComponent)st.get(DataComponentTypes.LORE);
        if (lorecomponent != null) {
            for (Text text : lorecomponent.comp_2400()) {
                stringbuilder.append(' ').append(text.getString().toLowerCase(Locale.ROOT));
            }
        }

        String s2 = stringbuilder.toString();

        for (String s1 : keys) {
            if (s2.contains(s1)) {
                return true;
            }
        }

        return false;
    }
}
