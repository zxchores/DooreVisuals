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
        "аукцион",
        "auction",
        "поиск",
        "хранилище",
        "ah",
        "аук",
        "продажи",
        "маркет",
        "мои лоты",
        "ваши лоты"
    };
    private static final Pattern PRICE = Pattern.compile(
        "(?i)(?:цена|price|стоимость|за\\s*шт|\\$)[^\\d]{0,8}(\\d[\\d\\s.,]*)"
    );
    private static final Pattern PLAIN = Pattern.compile("(\\d[\\d\\s]{2,})");
    private static final Pattern SECONDS = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:сек|sec|s)\\b");
    private static final Pattern RADIUS = Pattern.compile("(?i)(?:радиус|radius|r)\\D{0,8}(\\d+(?:[.,]\\d+)?)");
    private static final String[] DONATE = new String[]{
        "донат",
        "donate",
        "премиум",
        "premium",
        "легенд",
        "миф",
        "epic",
        "rare",
        "привилег",
        "рубин",
        "токен",
        "★",
        "✦"
    };

    private AuctionLore() {
    }

    public static boolean auctionScreen(MinecraftClient mc) {
        return ServerDetect.auction(mc);
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

    public static String blob(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            StringBuilder stringbuilder = new StringBuilder(stack.getName().getString());
            LoreComponent lorecomponent = (LoreComponent)stack.get(DataComponentTypes.LORE);
            if (lorecomponent != null) {
                for (Text text : lorecomponent.lines()) {
                    stringbuilder.append('\n').append(text.getString());
                }
            }

            return stringbuilder.toString();
        } else {
            return "";
        }
    }

    public static String displayName(ItemStack stack) {
        return stack != null && !stack.isEmpty() ? stack.getName().getString().replace('\u00a7', ' ').trim() : "";
    }

    public static boolean nameHas(ItemStack stack, String... needles) {
        String s = blob(stack).toLowerCase(Locale.ROOT);
        if (s.isBlank()) {
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

    public static boolean donate(ItemStack stack) {
        return nameHas(stack, DONATE);
    }

    public static Float loreSeconds(ItemStack stack) {
        String s = blob(stack).replace('\u00a7', ' ');
        Matcher matcher = SECONDS.matcher(s);
        if (!matcher.find()) {
            return null;
        } else {
            try {
                return Float.parseFloat(matcher.group(1).replace(',', '.'));
            } catch (Exception exception) {
                return null;
            }
        }
    }

    public static Double loreRadius(ItemStack stack) {
        String s = blob(stack).replace('\u00a7', ' ');
        Matcher matcher = RADIUS.matcher(s);
        if (!matcher.find()) {
            return null;
        } else {
            try {
                return Double.parseDouble(matcher.group(1).replace(',', '.'));
            } catch (Exception exception) {
                return null;
            }
        }
    }

    public static AuctionLore.Deal parse(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            long j = firstNumber(blob(stack));
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
