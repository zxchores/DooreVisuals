package dev.doorevisuals.draw;

public final class Theme {
    public static volatile Theme.Chrome CHROME = Theme.Chrome.GLASS;
    public static final float R = 12.0F;
    public static volatile float PANEL_A = 0.94F;
    public static volatile float CARD_A = 0.88F;
    public static volatile float NAV_A = 0.94F;
    public static volatile int ACCENT = -12533600;
    public static volatile int ACCENT_HOT = -8458284;
    public static volatile int ACCENT_DIM = -15046056;
    public static volatile int ACCENT_DEEP = -16378350;
    public static volatile int EFFECT = -12533600;
    public static volatile int HUD = -12533600;
    public static volatile int VOID = -16250870;
    public static volatile int BG = -670693365;
    public static volatile int PANEL = -301133547;
    public static volatile int PANEL_HI = -267118048;
    public static volatile int PANEL_LO = -267842544;
    public static volatile int HEADER = -401467620;
    public static volatile int ROW = -15854824;
    public static volatile int ROW_ON = -15390943;
    public static volatile int LINE = 687865855;
    public static volatile int TEXT = -986892;
    public static volatile int MUTED = -7697770;
    public static volatile int GHOST = -11908524;
    public static volatile int OK = -12714086;
    public static volatile int WARN = -18355;
    public static volatile int BAD = -50373;
    public static volatile int GLASS_BG = -804581613;
    public static volatile int GLASS_OUTLINE = 553648127;
    public static volatile int NEON_BG = -771355638;
    public static volatile int MINIMAL_BG = 2013924362;
    private static volatile int targetAccent = ACCENT;
    private static volatile int targetAccentHot = ACCENT_HOT;
    private static volatile int targetAccentDim = ACCENT_DIM;
    private static volatile int targetAccentDeep = ACCENT_DEEP;
    private static volatile int targetHud = HUD;
    private static volatile int targetEffect = EFFECT;

    private Theme() {
    }

    public static void tickTransition(float dt) {
        if (!(dt <= 0.0F)) {
            float f = (float)(1.0 - Math.pow(0.001F, dt * 6.0F));
            f = Math.min(1.0F, f);
            if (ACCENT != targetAccent) {
                ACCENT = lerpStep(ACCENT, targetAccent, f);
                ACCENT_HOT = lerpStep(ACCENT_HOT, targetAccentHot, f);
                ACCENT_DIM = lerpStep(ACCENT_DIM, targetAccentDim, f);
                ACCENT_DEEP = lerpStep(ACCENT_DEEP, targetAccentDeep, f);
                ROW_ON = alpha(lerp(ACCENT_DEEP, ACCENT, 0.25F), 255);
                retokenizeChrome(ACCENT_DEEP, ACCENT);
            }

            if (HUD != targetHud) {
                HUD = lerpStep(HUD, targetHud, f);
            }

            if (EFFECT != targetEffect) {
                EFFECT = lerpStep(EFFECT, targetEffect, f);
            }
        }
    }

    public static void applyAccent(int accent, int hot, int dim, int deep) {
        targetAccent = accent;
        targetAccentHot = hot;
        targetAccentDim = dim;
        targetAccentDeep = deep;
        targetHud = accent | 0xFF000000;
        targetEffect = accent | 0xFF000000;
        ACCENT = accent;
        ACCENT_HOT = hot;
        ACCENT_DIM = dim;
        ACCENT_DEEP = deep;
        ROW_ON = alpha(lerp(deep | 0xFF000000, accent, 0.22F), 255);
        HUD = accent | 0xFF000000;
        EFFECT = accent | 0xFF000000;
        retokenizeChrome(deep, accent);
    }

    public static void applyCustom(int gui, int hud, int effect) {
        applyMixed(gui, lerp(gui, -1, 0.35F), hud, effect);
    }

    public static void applyMixed(int primary, int secondary, int hud, int effect) {
        int i = primary | 0xFF000000;
        int j = secondary | 0xFF000000;
        int k = hud | 0xFF000000;
        int l = effect | 0xFF000000;
        int i1 = lerp(primary, -16777216, 0.45F);
        int j1 = lerp(primary, -16777216, 0.75F);
        targetAccent = i;
        targetAccentHot = j;
        targetAccentDim = i1;
        targetAccentDeep = j1;
        targetHud = k;
        targetEffect = l;
        ACCENT = i;
        ACCENT_HOT = j;
        HUD = k;
        EFFECT = l;
        ACCENT_DIM = i1;
        ACCENT_DEEP = j1;
        ROW_ON = alpha(lerp(ACCENT_DEEP, ACCENT, 0.25F), 255);
        retokenizeChrome(ACCENT_DEEP, ACCENT);
    }

    private static void retokenizeChrome(int deep, int accent) {
        PANEL = alpha(lerp(-15723502, deep, 0.35F), 240);
        PANEL_HI = alpha(lerp(PANEL, -1, 0.06F), 240);
        PANEL_LO = alpha(lerp(PANEL, -16777216, 0.18F), 240);
        HEADER = alpha(lerp(PANEL, accent, 0.08F), 240);
        ROW = alpha(lerp(PANEL, -16777216, 0.12F), 255);
        GLASS_BG = alpha(lerp(-15592428, deep, 0.4F), 200);
        NEON_BG = alpha(lerp(-16381938, deep, 0.55F), 210);
        MINIMAL_BG = alpha(lerp(-16119284, deep, 0.3F), 120);
        GLASS_OUTLINE = alpha(16777215, 28);
    }

    public static int alpha(int rgb, int a) {
        return Math.max(0, Math.min(255, a)) << 24 | rgb & 16777215;
    }

    public static int alpha(int rgb, float a) {
        return alpha(rgb, Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F));
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int i = a >>> 24 & 0xFF;
        int j = a >>> 16 & 0xFF;
        int k = a >>> 8 & 0xFF;
        int l = a & 0xFF;
        int i1 = b >>> 24 & 0xFF;
        int j1 = b >>> 16 & 0xFF;
        int k1 = b >>> 8 & 0xFF;
        int l1 = b & 0xFF;
        return (Math.round(i + (i1 - i) * t) & 0xFF) << 24
            | (Math.round(j + (j1 - j) * t) & 0xFF) << 16
            | (Math.round(k + (k1 - k) * t) & 0xFF) << 8
            | Math.round(l + (l1 - l) * t) & 0xFF;
    }

    private static int lerpStep(int current, int target, float rate) {
        return current == target ? target : lerp(current, target, rate);
    }

    public static int tintMul(int base, int tint, float mix) {
        mix = Math.max(0.0F, Math.min(1.0F, mix));
        int i = base >> 16 & 0xFF;
        int j = base >> 8 & 0xFF;
        int k = base & 0xFF;
        int l = tint >> 16 & 0xFF;
        int i1 = tint >> 8 & 0xFF;
        int j1 = tint & 0xFF;
        int k1 = Math.round(i * (1.0F - mix) + i * l / 255.0F * mix);
        int l1 = Math.round(j * (1.0F - mix) + j * i1 / 255.0F * mix);
        int i2 = Math.round(k * (1.0F - mix) + k * j1 / 255.0F * mix);
        return Math.min(255, k1) << 16 | Math.min(255, l1) << 8 | Math.min(255, i2);
    }

    public static enum Chrome {
        GLASS,
        DARK,
        NEON,
        MINIMAL,
        VANILLA;
    }
}
