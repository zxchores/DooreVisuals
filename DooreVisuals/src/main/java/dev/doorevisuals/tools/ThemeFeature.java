package dev.doorevisuals.tools;

import com.google.gson.JsonObject;
import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.data.GuiLayout;
import dev.doorevisuals.data.ThemesStore;
import dev.doorevisuals.draw.Skin;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.VisualQuality;
import dev.doorevisuals.overlay.HudFeature;

public final class ThemeFeature extends Feature {
    private static volatile boolean glassLive = true;
    private static volatile boolean glassUseTheme = true;
    private static volatile int glassTint = -12533600;
    private static volatile float glassPower = 0.85F;
    private static volatile boolean glintLive = true;
    private final Opt.Pick skin = this.opt(new Opt.Pick("skin", "\u041f\u0440\u0435\u0441\u0435\u0442", "Doore", Skin.labels()));
    private final Opt.Pick hudLook = this.opt(
        new Opt.Pick(
            "hud_look",
            "\u0421\u0442\u0438\u043b\u044c UI",
            "\u0421\u0442\u0435\u043a\u043b\u043e",
            "\u0421\u0442\u0435\u043a\u043b\u043e",
            "\u0422\u0451\u043c\u043d\u044b\u0439",
            "\u041d\u0435\u043e\u043d",
            "\u041c\u0438\u043d\u0438\u043c\u0430\u043b",
            "\u0412\u0430\u043d\u0438\u043b\u044c"
        )
    );
    private final Opt.Pick quality = this.opt(
        new Opt.Pick(
            "quality",
            "\u041a\u0430\u0447\u0435\u0441\u0442\u0432\u043e",
            "\u0412\u044b\u0441\u043e\u043a\u043e\u0435",
            "\u041d\u0438\u0437\u043a\u043e\u0435",
            "\u0421\u0440\u0435\u0434\u043d\u0435\u0435",
            "\u0412\u044b\u0441\u043e\u043a\u043e\u0435"
        )
    );
    private final Opt.Tint guiColor = this.opt(new Opt.Tint("gui", "ClickGUI", -12533600));
    private final Opt.Tint hudColor = this.opt(new Opt.Tint("hud", "HUD", -12533600));
    private final Opt.Tint effectColor = this.opt(new Opt.Tint("effect", "\u042d\u0444\u0444\u0435\u043a\u0442\u044b", -12533600));
    private final Opt.Tint secondaryColor = this.opt(new Opt.Tint("secondary", "\u0412\u0442\u043e\u0440\u043e\u0439", -8458284));
    private final Opt.Flag glassOn = this.opt(new Opt.Flag("glass", "\u0421\u0442\u0435\u043a\u043b\u043e", true));
    private final Opt.Flag glassTheme = this.opt(
        new Opt.Flag("glass_theme", "\u0421\u0442\u0435\u043a\u043b\u043e \u0438\u0437 \u0442\u0435\u043c\u044b", true).visibleWhen(this.glassOn::get)
    );
    private final Opt.Tint glassColor = this.opt(
        new Opt.Tint("glass_color", "\u0426\u0432\u0435\u0442 \u0441\u0442\u0435\u043a\u043b\u0430", -12533600)
            .visibleWhen(() -> (Boolean)this.glassOn.get() && !(Boolean)this.glassTheme.get())
    );
    private final Opt.Num glassStrength = this.opt(
        new Opt.Num("glass_strength", "\u0421\u0438\u043b\u0430 \u0441\u0442\u0435\u043a\u043b\u0430", 0.85, 0.35, 1.2, 0.05).visibleWhen(this.glassOn::get)
    );
    private final Opt.Flag themeGlint = this.opt(
        new Opt.Flag("theme_glint", "\u0427\u0430\u0440\u044b \u0446\u0432\u0435\u0442\u043e\u043c \u0442\u0435\u043c\u044b", true)
    );
    private final Opt.Num panelAlpha = this.opt(
        new Opt.Num(
            "panel_a",
            "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u043f\u0430\u043d\u0435\u043b\u0435\u0439",
            0.94,
            0.45,
            1.0,
            0.05
        )
    );
    private final Opt.Num cardAlpha = this.opt(
        new Opt.Num(
            "card_a",
            "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u043a\u0430\u0440\u0442\u043e\u0447\u0435\u043a",
            0.88,
            0.35,
            1.0,
            0.05
        )
    );
    private final Opt.Num navAlpha = this.opt(
        new Opt.Num(
            "nav_a",
            "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u0441\u0430\u0439\u0434\u0431\u0430\u0440\u0430",
            0.94,
            0.45,
            1.0,
            0.05
        )
    );
    private String lastPreset = "";
    private String draftName = "my-theme";
    private int editChannel;

    public ThemeFeature() {
        super(
            "theme",
            "Theme",
            "\u0426\u0432\u0435\u0442\u0430 ClickGUI \u00b7 HUD \u00b7 \u044d\u0444\u0444\u0435\u043a\u0442\u044b \u00b7 \u043a\u0430\u0447\u0435\u0441\u0442\u0432\u043e \u00b7 \u0441\u043c\u0435\u0448\u0430\u043d\u043d\u044b\u0435 \u043f\u0440\u0435\u0441\u0435\u0442\u044b",
            Category.THEME,
            true
        );
        this.apply(true);
    }

    public static int effectTint() {
        return Theme.EFFECT;
    }

    public static int hudTint() {
        return Theme.HUD;
    }

    public static boolean glassActive() {
        return glassLive;
    }

    public static int glassTint() {
        return glassUseTheme ? hudTint() : glassTint;
    }

    public static float glassStrength() {
        return glassPower;
    }

    public static boolean themeGlint() {
        return glintLive;
    }

    public String draftName() {
        return this.draftName;
    }

    public String presetLabel() {
        return (String)this.skin.get();
    }

    public void setDraftName(String n) {
        this.draftName = n == null ? "" : n;
    }

    public int editChannel() {
        return this.editChannel;
    }

    public void setEditChannel(int ch) {
        this.editChannel = Math.max(0, Math.min(3, ch));
    }

    public int channelColor(int ch) {
        return switch (ch) {
            case 1 -> this.hudColor.get();
            case 2 -> this.effectColor.get();
            case 3 -> this.secondaryColor.get();
            default -> this.guiColor.get();
        };
    }

    public void setChannelColor(int ch, int argb) {
        int i = argb | 0xFF000000;
        switch (ch) {
            case 1:
                this.hudColor.set(i);
                break;
            case 2:
                this.effectColor.set(i);
                break;
            case 3:
                this.secondaryColor.set(i);
                break;
            default:
                this.guiColor.set(i);
        }

        this.skin.set("\u0421\u0432\u043e\u044f");
        this.lastPreset = "\u0421\u0432\u043e\u044f";
        Theme.applyMixed((Integer)this.guiColor.get(), (Integer)this.secondaryColor.get(), (Integer)this.hudColor.get(), (Integer)this.effectColor.get());
        this.poke();
    }

    public Opt.Tint channelTint(int ch) {
        return switch (ch) {
            case 1 -> this.hudColor;
            case 2 -> this.effectColor;
            case 3 -> this.secondaryColor;
            default -> this.guiColor;
        };
    }

    public void applyPreset(String label) {
        this.skin.set(label);
        this.apply(true);
        this.poke();
    }

    public void applyMixed(ThemesStore.MixedPreset p) {
        this.guiColor.set(p.primary());
        this.hudColor.set(p.primary());
        this.effectColor.set(p.secondary());
        this.secondaryColor.set(p.secondary());
        this.skin.set("\u0421\u0432\u043e\u044f");
        this.lastPreset = "\u0421\u0432\u043e\u044f";
        ThemesStore.applyMixedPreset(p);
        this.poke();
        this.notify("\u0422\u0435\u043c\u0430", p.label());
    }

    public boolean saveDraft() {
        boolean flag = ThemesStore.save(
            this.draftName, (Integer)this.guiColor.get(), (Integer)this.hudColor.get(), (Integer)this.effectColor.get(), (Integer)this.secondaryColor.get()
        );
        if (flag) {
            this.skin.set("\u0421\u0432\u043e\u044f");
            this.notify("\u0422\u0435\u043c\u0430", "\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0430: " + ThemesStore.clean(this.draftName));
        } else {
            this.notify("\u0422\u0435\u043c\u0430", "\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f");
        }

        return flag;
    }

    public boolean applyNamed(String name) {
        JsonObject jsonobject = ThemesStore.loadRaw(name);
        if (jsonobject == null) {
            this.notify("\u0422\u0435\u043c\u0430", "\u041d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430: " + name);
            return false;
        } else {
            this.guiColor.set(jsonobject.has("gui") ? jsonobject.get("gui").getAsInt() : Theme.ACCENT);
            this.hudColor.set(jsonobject.has("hud") ? jsonobject.get("hud").getAsInt() : (Integer)this.guiColor.get());
            this.effectColor.set(jsonobject.has("effect") ? jsonobject.get("effect").getAsInt() : (Integer)this.guiColor.get());
            this.secondaryColor.set(jsonobject.has("secondary") ? jsonobject.get("secondary").getAsInt() : Theme.ACCENT_HOT);
            this.skin.set("\u0421\u0432\u043e\u044f");
            this.lastPreset = "\u0421\u0432\u043e\u044f";
            ThemesStore.applyJson(jsonobject);
            this.draftName = name;
            this.poke();
            this.notify("\u0422\u0435\u043c\u0430", "\u041f\u0440\u0438\u043c\u0435\u043d\u0435\u043d\u0430: " + name);
            return true;
        }
    }

    public boolean deleteNamed(String name) {
        boolean flag = ThemesStore.delete(name);
        this.notify(
            "\u0422\u0435\u043c\u0430",
            flag ? "\u0423\u0434\u0430\u043b\u0435\u043d\u0430: " + name : "\u041d\u0435 \u0443\u0434\u0430\u043b\u0435\u043d\u0430"
        );
        return flag;
    }

    @Override
    protected void enable() {
        this.apply(true);
    }

    @Override
    public void poke() {
        super.poke();
        this.apply(false);
    }

    private void apply(boolean forceSyncTints) {
        Theme.PANEL_A = this.panelAlpha.f();
        Theme.CARD_A = this.cardAlpha.f();
        Theme.NAV_A = this.navAlpha.f();
        glassLive = (Boolean)this.glassOn.get();
        glassUseTheme = (Boolean)this.glassTheme.get();
        glassTint = (Integer)this.glassColor.get();
        glassPower = this.glassStrength.f();
        glintLive = (Boolean)this.themeGlint.get();
        String s = (String)this.hudLook.get();

        Theme.CHROME = switch (s) {
            case "\u041d\u0435\u043e\u043d" -> Theme.Chrome.NEON;
            case "\u041c\u0438\u043d\u0438\u043c\u0430\u043b" -> Theme.Chrome.MINIMAL;
            case "\u0422\u0451\u043c\u043d\u044b\u0439" -> Theme.Chrome.DARK;
            case "\u0412\u0430\u043d\u0438\u043b\u044c" -> Theme.Chrome.VANILLA;
            default -> Theme.Chrome.GLASS;
        };
        if ((Boolean)this.glassOn.get()
            && !"\u041d\u0435\u043e\u043d".equals(this.hudLook.get())
            && !"\u041c\u0438\u043d\u0438\u043c\u0430\u043b".equals(this.hudLook.get())
            && !"\u0422\u0451\u043c\u043d\u044b\u0439".equals(this.hudLook.get())
            && !"\u0412\u0430\u043d\u0438\u043b\u044c".equals(this.hudLook.get())) {
            Theme.CHROME = Theme.Chrome.GLASS;
        }

        if (GuiLayout.uniformPanels) {
            Theme.CHROME = Theme.Chrome.DARK;
        }

        VisualQuality.setLabel((String)this.quality.get());
        s = (String)this.skin.get();
        if ("\u0421\u0432\u043e\u044f".equals(s)) {
            Theme.applyMixed((Integer)this.guiColor.get(), (Integer)this.secondaryColor.get(), (Integer)this.hudColor.get(), (Integer)this.effectColor.get());
            this.lastPreset = s;
        } else {
            Skin skin = Skin.byLabel(s);
            skin.apply();
            Theme.HUD = skin.accent();
            Theme.EFFECT = skin.accent();
            if (forceSyncTints || !s.equals(this.lastPreset)) {
                this.guiColor.set(skin.accent());
                this.hudColor.set(skin.accent());
                this.effectColor.set(skin.accent());
                this.secondaryColor.set(Theme.ACCENT_HOT);
            }

            this.lastPreset = s;
        }
    }

    public void reapplyChrome() {
        this.apply(false);
    }

    public void setHudLook(String look) {
        this.hudLook.set(look);
        this.apply(false);
        this.poke();
    }

    public String hudLook() {
        return (String)this.hudLook.get();
    }

    public String qualityLabel() {
        return (String)this.quality.get();
    }

    public void setQualityLabel(String label) {
        this.quality.set(label);
        this.apply(false);
        this.poke();
    }

    private void notify(String t, String b) {
        App.features().find(HudFeature.class).ifPresent(h -> h.notify(t, b));
    }
}
