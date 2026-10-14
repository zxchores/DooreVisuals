package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import net.minecraft.client.gui.DrawContext;

public final class ChatFeature extends Feature {
    private static volatile boolean live;
    private static volatile boolean glassBg = true;
    private static volatile int accent = Theme.ACCENT;
    private static volatile float strength = 0.72F;
    private static volatile boolean bar = true;
    private static volatile boolean timestamps = true;
    private static volatile boolean mentionHighlight = true;
    private static volatile boolean antiSpam = true;
    private static volatile boolean infiniteHistory = true;
    private static volatile int customWidth = 320;
    private static volatile int tsColor = -7697770;
    private static volatile int mentionColor = -14262;
    private static volatile int maxHistory = 1000;
    private final Opt.Flag glass = this.opt(new Opt.Flag("glass", "\u0421\u0442\u0435\u043a\u043b\u044f\u043d\u043d\u044b\u0439 \u0444\u043e\u043d", true));
    private final Opt.Flag accentBar = this.opt(
        new Opt.Flag("accent_bar", "\u041f\u043e\u043b\u043e\u0441\u0430 \u0430\u043a\u0446\u0435\u043d\u0442\u0430", true)
    );
    private final Opt.Num opacity = this.opt(
        new Opt.Num("opacity", "\u041d\u0435\u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c", 0.72, 0.25, 1.0, 0.05)
    );
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme_color", "\u0426\u0432\u0435\u0442 \u0438\u0437 \u0442\u0435\u043c\u044b", true));
    private final Opt.Flag tsOpt = this.opt(
        new Opt.Flag("timestamps", "\u0412\u0440\u0435\u043c\u044f \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0439", true)
    );
    private final Opt.Flag mentionOpt = this.opt(
        new Opt.Flag("mentions", "\u041f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0430 \u0443\u043f\u043e\u043c\u0438\u043d\u0430\u043d\u0438\u0439", true)
    );
    private final Opt.Flag antiSpamOpt = this.opt(new Opt.Flag("anti_spam", "\u0421\u0436\u0430\u0442\u0438\u0435 \u0441\u043f\u0430\u043c\u0430", true));
    private final Opt.Flag infiniteOpt = this.opt(
        new Opt.Flag("infinite", "\u0411\u0435\u0441\u043a\u043e\u043d\u0435\u0447\u043d\u0430\u044f \u0438\u0441\u0442\u043e\u0440\u0438\u044f", true)
    );
    private final Opt.Num widthOpt = this.opt(new Opt.Num("width", "\u0428\u0438\u0440\u0438\u043d\u0430 \u0447\u0430\u0442\u0430", 320.0, 280.0, 640.0, 10.0));
    private final Opt.Num maxHistOpt = this.opt(
        new Opt.Num("max_history", "\u041b\u0438\u043c\u0438\u0442 \u0438\u0441\u0442\u043e\u0440\u0438\u0438", 1000.0, 100.0, 5000.0, 100.0)
    );
    private final Opt.Tint tsColorOpt = this.opt(new Opt.Tint("ts_color", "\u0426\u0432\u0435\u0442 \u0432\u0440\u0435\u043c\u0435\u043d\u0438", -7697770));
    private final Opt.Tint mentionColorOpt = this.opt(
        new Opt.Tint("mention_color", "\u0426\u0432\u0435\u0442 \u0443\u043f\u043e\u043c\u0438\u043d\u0430\u043d\u0438\u0439", -14262)
    );

    public ChatFeature() {
        super(
            "chat",
            "Chat",
            "\u0421\u0442\u0435\u043a\u043b\u044f\u043d\u043d\u044b\u0439 \u0444\u043e\u043d, \u0432\u0440\u0435\u043c\u044f, \u0443\u043f\u043e\u043c\u0438\u043d\u0430\u043d\u0438\u044f, \u0430\u043d\u0442\u0438-\u0441\u043f\u0430\u043c, \u0438\u0441\u0442\u043e\u0440\u0438\u044f",
            Category.OVERLAY,
            true
        );
    }

    public static boolean active() {
        return live;
    }

    public static boolean glassBackground() {
        return live && glassBg;
    }

    public static boolean timestamps() {
        return live && timestamps;
    }

    public static boolean mentionHighlight() {
        return live && mentionHighlight;
    }

    public static boolean antiSpam() {
        return live && antiSpam;
    }

    public static boolean infiniteHistory() {
        return live && infiniteHistory;
    }

    public static int customWidth() {
        return customWidth;
    }

    public static int tsColor() {
        return tsColor;
    }

    public static int mentionColor() {
        return mentionColor;
    }

    public static int maxHistory() {
        return maxHistory;
    }

    public static void paintBackground(DrawContext g, int sw, int sh, int chatHeight) {
        if (live && glassBg && chatHeight > 0) {
            float f = Math.min(customWidth, Math.max(120, sw - 8));
            float f1 = sh - chatHeight - 2;
            float f2 = chatHeight + 2;
            if (ThemeFeature.glassActive()) {
                Paint.glass(g, 2.0F, f1, f, f2, 12.0F);
            } else {
                int i = Theme.alpha(659988, Math.round(200.0F * strength));
                Paint.box(g, 2.0F, f1, f, f2, i, 12.0F);
            }

            if (bar) {
                Paint.box(g, 2.0F, f1, 2.4F, f2, Theme.alpha(accent, 180), 0.0F);
            }
        }
    }

    public static String formatTimestamp() {
        long i = System.currentTimeMillis();
        int j = (int)(i / 3600000L % 24L);
        int k = (int)(i / 60000L % 60L);
        return String.format("[%02d:%02d] ", j, k);
    }

    public static boolean isMention(String message, String username) {
        return mentionHighlight && message != null && username != null && !username.isEmpty() ? message.toLowerCase().contains(username.toLowerCase()) : false;
    }

    @Override
    protected void enable() {
        live = true;
        this.sync();
    }

    @Override
    protected void disable() {
        live = false;
    }

    @Override
    public void poke() {
        super.poke();
        if (this.on()) {
            live = true;
            this.sync();
        }
    }

    private void sync() {
        glassBg = (Boolean)this.glass.get();
        strength = this.opacity.f();
        accent = this.themeColor.get() ? ThemeFeature.hudTint() : Theme.ACCENT;
        bar = (Boolean)this.accentBar.get();
        timestamps = (Boolean)this.tsOpt.get();
        mentionHighlight = (Boolean)this.mentionOpt.get();
        antiSpam = (Boolean)this.antiSpamOpt.get();
        infiniteHistory = (Boolean)this.infiniteOpt.get();
        customWidth = this.widthOpt.i();
        maxHistory = this.maxHistOpt.i();
        tsColor = (Integer)this.tsColorOpt.get();
        mentionColor = (Integer)this.mentionColorOpt.get();
    }
}
