package dev.doorevisuals.tools;

import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public final class InvClicks {
    private static final Queue<InvClicks.Click> Q = new ArrayDeque<>();
    private static int wait;

    private InvClicks() {
    }

    public static boolean busy() {
        return wait > 0 || !Q.isEmpty();
    }

    public static void clear() {
        Q.clear();
        wait = 0;
    }

    public static void pickup(int menuId, int slot) {
        Q.add(new InvClicks.Click(menuId, slot, 0, SlotActionType.PICKUP));
    }

    public static void throwAll(int menuId, int slot) {
        Q.add(new InvClicks.Click(menuId, slot, 1, SlotActionType.THROW));
    }

    public static void swap(int menuId, int a, int b) {
        if (a != b) {
            pickup(menuId, a);
            pickup(menuId, b);
            pickup(menuId, a);
        }
    }

    public static void tick(MinecraftClient mc) {
        if (wait > 0) {
            wait--;
        } else if (!Q.isEmpty() && mc.interactionManager != null && mc.player != null) {
            InvClicks.Click invclicks$click = Q.poll();
            if (invclicks$click != null) {
                ClientPlayerEntity clientplayerentity = mc.player;
                ScreenHandler screenhandler = clientplayerentity.currentScreenHandler;
                int i = invclicks$click.menu;
                if (i == 0) {
                    ScreenHandler playerscreenhandler = clientplayerentity.playerScreenHandler;
                    i = playerscreenhandler.syncId;
                } else if (screenhandler.syncId != i) {
                    return;
                }

                mc.interactionManager.clickSlot(i, invclicks$click.slot, invclicks$click.button, invclicks$click.type, clientplayerentity);
                wait = 2;
            }
        }
    }

    private record Click(int menu, int slot, int button, SlotActionType type) {
    }
}
