package dev.doorevisuals.tools;

import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

public final class ServerDetect {
    public static final String[] HOSTS = new String[]{"funtime", "spookytime", "funsky", "hollyworld", "spacetime"};
    private static final String[] AUCTION = new String[]{
        "аукцион",
        "auction",
        "поиск",
        "хранилище",
        "ah",
        "аук",
        "продажи",
        "маркет"
    };
    private static final String[] OWN = new String[]{
        "мои лоты",
        "ваши лоты",
        "мои продажи",
        "ваши продажи",
        "my lots",
        "your lots",
        "снятые",
        "активные лоты"
    };
    private static final String[] TREASURY = new String[]{"казна", "клан", "пополнить казн", "treasury", "clan bank", "clan treasury"};
    private static final String[] EVENTS = new String[]{"ивент", "события", "event", "анархия", "anarchy", "ивенты"};
    private static final String[] CONFIRM = new String[]{"подтверд", "confirm", "вы уверены", "are you sure", "подтвердить"};

    private ServerDetect() {
    }

    public static boolean host(MinecraftClient mc) {
        return AuctionLore.serverMatch(mc, HOSTS);
    }

    public static String title(MinecraftClient mc) {
        Screen screen = mc == null ? null : mc.currentScreen;
        if (screen == null || screen.getTitle() == null) {
            return "";
        } else {
            return screen.getTitle().getString().toLowerCase(Locale.ROOT);
        }
    }

    public static boolean titleHas(MinecraftClient mc, String... needles) {
        String s = title(mc);
        if (s.isEmpty()) {
            return false;
        } else {
            for (String s1 : needles) {
                if (s.contains(s1.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }

            return false;
        }
    }

    public static ServerDetect.Kind window(MinecraftClient mc) {
        String s = title(mc);
        if (s.isEmpty()) {
            return ServerDetect.Kind.NONE;
        } else if (containsAny(s, CONFIRM)) {
            return ServerDetect.Kind.CONFIRM;
        } else if (containsAny(s, OWN)) {
            return ServerDetect.Kind.OWN_LOTS;
        } else if (containsAny(s, TREASURY)) {
            return ServerDetect.Kind.TREASURY;
        } else if (containsAny(s, EVENTS)) {
            return ServerDetect.Kind.EVENTS;
        } else {
            return containsAny(s, AUCTION) ? ServerDetect.Kind.AUCTION : ServerDetect.Kind.NONE;
        }
    }

    public static boolean auction(MinecraftClient mc) {
        ServerDetect.Kind serverdetect$kind = window(mc);
        return serverdetect$kind == ServerDetect.Kind.AUCTION || serverdetect$kind == ServerDetect.Kind.OWN_LOTS;
    }

    public static int findSlot(ScreenHandler menu, String... needles) {
        if (menu == null) {
            return -1;
        } else {
            for (Slot slot : menu.slots) {
                if (AuctionLore.nameHas(slot.getStack(), needles)) {
                    return slot.id;
                }
            }

            return -1;
        }
    }

    public static void command(MinecraftClient mc, String command) {
        if (mc != null && mc.player != null && mc.player.networkHandler != null && command != null && !command.isBlank()) {
            String s = command.startsWith("/") ? command.substring(1) : command;

            try {
                mc.player.networkHandler.sendChatCommand(s);
            } catch (Throwable throwable) {
                try {
                    mc.player.networkHandler.sendChatMessage("/" + s);
                } catch (Throwable throwable1) {
                }
            }
        }
    }

    private static boolean containsAny(String blob, String[] needles) {
        for (String s : needles) {
            if (blob.contains(s)) {
                return true;
            }
        }

        return false;
    }

    public static enum Kind {
        NONE,
        AUCTION,
        OWN_LOTS,
        TREASURY,
        EVENTS,
        CONFIRM;
    }
}
