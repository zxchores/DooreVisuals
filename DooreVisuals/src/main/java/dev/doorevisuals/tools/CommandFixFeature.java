package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import java.util.Locale;

public final class CommandFixFeature extends Feature {
    private static final String RU = "йцукенгшщзхъфывапролджэячсмитьбю.ёЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ,Ё";
    private static final String EN = "qwertyuiop[]asdfghjkl;'zxcvbnm,./`QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>?~";

    public CommandFixFeature() {
        super("command_fix", "Command Fix", "ЙЦУКЕН-команды отправляются латиницей", Category.TOOLS, true);
    }

    public static String rewrite(String raw) {
        if (raw == null || !raw.startsWith("/")) {
            return raw;
        } else {
            CommandFixFeature commandfixfeature = App.features().find(CommandFixFeature.class).filter(Feature::on).orElse(null);
            if (commandfixfeature == null) {
                return raw;
            } else {
                String s = map(raw);
                return looksLatin(s) && !s.equals(raw) ? s : raw;
            }
        }
    }

    private static String map(String raw) {
        StringBuilder stringbuilder = new StringBuilder(raw.length());

        for (int i = 0; i < raw.length(); i++) {
            char c0 = raw.charAt(i);
            int j = RU.indexOf(c0);
            stringbuilder.append(j >= 0 && j < EN.length() ? EN.charAt(j) : c0);
        }

        return stringbuilder.toString();
    }

    private static boolean looksLatin(String mapped) {
        int i = mapped.indexOf(' ');
        String s = (i < 0 ? mapped : mapped.substring(0, i)).toLowerCase(Locale.ROOT);
        if (s.length() < 2 || s.charAt(0) != '/') {
            return false;
        } else {
            for (int j = 1; j < s.length(); j++) {
                char c0 = s.charAt(j);
                if (c0 != '_' && (c0 < 'a' || c0 > 'z') && (c0 < '0' || c0 > '9')) {
                    return false;
                }
            }

            return true;
        }
    }
}
