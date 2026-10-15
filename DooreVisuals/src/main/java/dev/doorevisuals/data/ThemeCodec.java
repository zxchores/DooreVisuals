package dev.doorevisuals.data;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** A theme as a short text code that can be copied and pasted: DVT1:... holds the four colours. */
public final class ThemeCodec {
    private static final String PREFIX = "DVT1:";

    private ThemeCodec() {
    }

    /** The code of a theme: gui, secondary, hud and effect colours. */
    public static String encode(int gui, int secondary, int hud, int effect) {
        String plain = gui + "," + secondary + "," + hud + "," + effect;
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    /** The four colours (gui, secondary, hud, effect) of a code, or null when the text is not a theme code. */
    public static int[] decode(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (!t.startsWith(PREFIX)) {
            return null;
        }
        try {
            String plain = new String(Base64.getUrlDecoder().decode(t.substring(PREFIX.length())), StandardCharsets.UTF_8);
            String[] parts = plain.split(",");
            if (parts.length != 4) {
                return null;
            }
            int[] out = new int[4];
            for (int i = 0; i < 4; i++) {
                out[i] = Integer.parseInt(parts[i].trim());
            }
            return out;
        } catch (IllegalArgumentException error) {
            return null;
        }
    }
}
