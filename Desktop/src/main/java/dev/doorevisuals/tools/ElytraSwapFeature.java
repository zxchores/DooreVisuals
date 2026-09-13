package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

public final class ElytraSwapFeature extends Feature implements Tick {
    private static final int CHEST = 6;
    private final Opt.Key swap = this.opt(new Opt.Key("swap", "\u0421\u0432\u0430\u043f", -1));
    private boolean wasDown;

    public ElytraSwapFeature() {
        super(
            "elytra_swap",
            "Elytra Swap",
            "\u0413\u0440\u0443\u0434\u044c \u2194 \u044d\u043b\u0438\u0442\u0440\u044b \u0432 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u0435",
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
                if (flag && !this.wasDown && !InvClicks.busy()) {
                    run(mc);
                }

                this.wasDown = flag;
            } else {
                this.wasDown = false;
            }
        } else {
            this.wasDown = false;
        }
    }

    private static void run(MinecraftClient mc) {
        PlayerScreenHandler playerscreenhandler = mc.player.playerScreenHandler;
        if (playerscreenhandler.slots.size() > 6) {
            ItemStack itemstack = ((Slot)playerscreenhandler.slots.get(6)).getStack();
            boolean flag = itemstack.isOf(Items.ELYTRA);
            int i = -1;

            for (int j = 9; j <= 44 && j < playerscreenhandler.slots.size(); j++) {
                ItemStack itemstack1 = ((Slot)playerscreenhandler.slots.get(j)).getStack();
                if (!itemstack1.isEmpty()) {
                    if (flag) {
                        if (!itemstack1.isOf(Items.ELYTRA) && looksLikeChest(itemstack1)) {
                            i = j;
                            break;
                        }
                    } else if (itemstack1.isOf(Items.ELYTRA)) {
                        i = j;
                        break;
                    }
                }
            }

            if (i >= 0) {
                InvClicks.swap(0, 6, i);
            }
        }
    }

    private static boolean looksLikeChest(ItemStack st) {
        String s = Registries.ITEM.getId(st.getItem()).getPath();
        return s.contains("chestplate") || s.contains("elytra");
    }
}
