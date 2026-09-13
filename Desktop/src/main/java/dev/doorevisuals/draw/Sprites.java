package dev.doorevisuals.draw;

import net.minecraft.util.Identifier;

public final class Sprites {
    public static final Identifier DOT = id("dot.png");
    public static final Identifier HEART = id("heart.png");
    public static final Identifier STAR = id("star.png");
    public static final Identifier DOLLAR = id("dollar.png");
    public static final Identifier DIGIT_6 = id("digit_6.png");
    public static final Identifier DIGIT_7 = id("digit_7.png");
    public static final Identifier ICON = id("icon.png");
    public static final Identifier GLOW = id("glow.png");
    public static final Identifier SOFT_GLOW_ATLAS = id("soft_glow_atlas.png");
    public static final Identifier DOORE_BADGE = Identifier.of("doorevisuals", "textures/gui/doore_badge.png");
    public static final Identifier LOGO = Identifier.of("doorevisuals", "textures/gui/logo_round.png");
    public static final Identifier CLIENT_ICON = Identifier.of("doorevisuals", "textures/gui/client_icon.png");
    public static final Identifier MENU_BG = Identifier.of("doorevisuals", "textures/gui/menu_bg.png");

    private Sprites() {
    }

    private static Identifier id(String file) {
        return Identifier.of("doorevisuals", "textures/fx/" + file);
    }
}
