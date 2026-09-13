package dev.doorevisuals.tools;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class AuctionLore {
    public static final String[] TITLES = new String[]{
        "\u0430\u0443\u043a\u0446\u0438\u043e\u043d",
        "auction",
        "\u043f\u043e\u0438\u0441\u043a",
        "\u0445\u0440\u0430\u043d\u0438\u043b\u0438\u0449\u0435",
        "ah",
        "\u0430\u0443\u043a",
        "\u043f\u0440\u043e\u0434\u0430\u0436\u0438",
        "\u043c\u0430\u0440\u043a\u0435\u0442"
    };
    private static final Pattern PRICE = Pattern.compile(
        "(?i)(?:\u0446\u0435\u043d\u0430|price|\u0441\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c|\u0437\u0430\\s*\u0448\u0442|\\$)[^\\d]{0,8}(\\d[\\d\\s.,]*)"
    );
    private static final Pattern PLAIN = Pattern.compile("(\\d[\\d\\s]{2,})");

    private AuctionLore() {
    }

    public static boolean auctionScreen(MinecraftClient mc) {
        if (mc.currentScreen == null) {
            return false;
        } else {
            String s = mc.currentScreen.getTitle() == null ? "" : mc.currentScreen.getTitle().getString().toLowerCase(Locale.ROOT);

            for (String s1 : TITLES) {
                if (s.contains(s1)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean serverMatch(MinecraftClient mc, String... needles) {
        try {
            if (mc.getCurrentServerEntry() == null || mc.getCurrentServerEntry().address == null) {
                return false;
            }

            String s = mc.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);

            for (String s1 : needles) {
                if (s.contains(s1)) {
                    return true;
                }
            }
        } catch (Throwable throwable) {
        }

        return false;
    }

    public static AuctionLore.Deal parse(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            String s = stack.getName().getString();
            LoreComponent lorecomponent = (LoreComponent)stack.get(DataComponentTypes.LORE);
            if (lorecomponent != null) {
                for (Text text : lorecomponent.lines()) {
                    s = s + "\n" + text.getString();
                }
            }

            long j = firstNumber(s);
            if (j < 0L) {
                return null;
            } else {
                int i = Math.max(1, stack.getCount());
                return new AuctionLore.Deal(j, i, (double)j / i);
            }
        } else {
            return null;
        }
    }

    private static long firstNumber(String blob) {
        Matcher matcher = PRICE.matcher(blob);
        if (matcher.find()) {
            return digits(matcher.group(1));
        } else {
            Matcher matcher1 = PLAIN.matcher(blob.replace('\u00a7', ' '));
            return matcher1.find() ? digits(matcher1.group(1)) : -1L;
        }
    }

    private static long digits(String raw) {
        String s = raw.replaceAll("[^0-9]", "");
        if (!s.isEmpty() && s.length() <= 15) {
            try {
                return Long.parseLong(s);
            } catch (Exception exception) {
                return -1L;
            }
        } else {
            return -1L;
        }
    }

    public record Deal(long total, int count, double each) {
    }
}
