package dev.doorevisuals.overlay;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Spring;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.util.math.ColorHelper;

public final class HudTweaksFeature extends Feature implements Tick {
    private static volatile boolean sbHide = false;
    private static volatile float sbScale = 1.0F;
    private static volatile int sbOpacity = 255;
    private static volatile boolean sbHideScores = false;
    private static volatile float sbOffsetX = 0.0F;
    private static volatile float sbOffsetY = 0.0F;
    private static volatile boolean bbHide = false;
    private static volatile float bbScale = 1.0F;
    private static volatile int bbStyle = 0;
    private static volatile float bbOffsetX = 0.0F;
    private static volatile float bbOffsetY = 0.0F;
    private static volatile boolean bbHideName = false;
    private static final ThreadLocal<Boolean> SIDEBAR = ThreadLocal.withInitial(() -> false);
    private static volatile Map<UUID, ClientBossBar> bossEvents;
    private final Opt.Flag sbOn = this.opt(new Opt.Flag("sb_enabled", "Scoreboard", true));
    private final Opt.Num sbScaleOpt = this.opt(new Opt.Num("sb_scale", "SB \u043c\u0430\u0441\u0448\u0442\u0430\u0431", 1.0, 0.5, 1.5, 0.05));
    private final Opt.Num sbOpacityOpt = this.opt(
        new Opt.Num("sb_opacity", "SB \u043d\u0435\u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c", 255.0, 0.0, 255.0, 5.0)
    );
    private final Opt.Flag sbHideScoresOpt = this.opt(new Opt.Flag("sb_hide_scores", "SB \u0441\u043a\u0440\u044b\u0442\u044c \u043e\u0447\u043a\u0438", false));
    private final Opt.Num sbOffsetXOpt = this.opt(new Opt.Num("sb_ox", "SB \u0441\u0434\u0432\u0438\u0433 X", 0.0, -200.0, 200.0, 1.0));
    private final Opt.Num sbOffsetYOpt = this.opt(new Opt.Num("sb_oy", "SB \u0441\u0434\u0432\u0438\u0433 Y", 0.0, -200.0, 200.0, 1.0));
    private final Opt.Flag bbOn = this.opt(new Opt.Flag("bb_enabled", "BossBar", true));
    private final Opt.Num bbScaleOpt = this.opt(new Opt.Num("bb_scale", "BB \u043c\u0430\u0441\u0448\u0442\u0430\u0431", 1.0, 0.5, 2.0, 0.05));
    private final Opt.Pick bbStyleOpt = this.opt(
        new Opt.Pick("bb_style", "BB \u0441\u0442\u0438\u043b\u044c", "default", "default", "thin", "segmented", "text_only")
    );
    private final Opt.Num bbOffsetXOpt = this.opt(new Opt.Num("bb_ox", "BB \u0441\u0434\u0432\u0438\u0433 X", 0.0, -400.0, 400.0, 1.0));
    private final Opt.Num bbOffsetYOpt = this.opt(new Opt.Num("bb_oy", "BB \u0441\u0434\u0432\u0438\u0433 Y", 0.0, -200.0, 200.0, 1.0));
    private final Opt.Flag bbHideNameOpt = this.opt(new Opt.Flag("bb_hide_name", "BB \u0441\u043a\u0440\u044b\u0442\u044c \u0438\u043c\u044f", false));
    private final Spring reveal = new Spring(0.0F);

    public HudTweaksFeature() {
        super(
            "hud_tweaks",
            "HUD Tweaks",
            "\u041a\u0430\u0441\u0442\u043e\u043c\u0438\u0437\u0430\u0446\u0438\u044f \u0441\u043a\u043e\u0440\u0431\u043e\u0440\u0434\u0430 \u0438 \u043f\u043e\u043b\u043e\u0441\u044b \u0431\u043e\u0441\u0441\u0430",
            Category.OVERLAY,
            true
        );
        this.unlist();
    }

    public static boolean scoreboardHidden() {
        return sbHide;
    }

    public static float scoreboardScale() {
        return sbScale;
    }

    public static int scoreboardOpacity() {
        return sbOpacity;
    }

    public static boolean scoreboardHideScores() {
        return sbHideScores;
    }

    public static float scoreboardOffsetX() {
        return sbOffsetX;
    }

    public static float scoreboardOffsetY() {
        return sbOffsetY;
    }

    public static boolean bossBarHidden() {
        return bbHide;
    }

    public static float bossBarScale() {
        return bbScale;
    }

    public static int bossBarStyle() {
        return bbStyle;
    }

    public static float bossBarOffsetX() {
        return bbOffsetX;
    }

    public static float bossBarOffsetY() {
        return bbOffsetY;
    }

    public static boolean bossBarHideName() {
        return bbHideName;
    }

    public static void sidebarBegin() {
        SIDEBAR.set(true);
    }

    public static void sidebarEnd() {
        SIDEBAR.set(false);
    }

    public static boolean sidebarPaint() {
        return Boolean.TRUE.equals(SIDEBAR.get());
    }

    public static int sidebarTint(int color) {
        if (!sidebarPaint()) {
            return color;
        } else {
            int i = Math.max(0, Math.min(255, sbOpacity));
            return i >= 255 ? color : ColorHelper.mix(color, ColorHelper.getArgb(i, 255, 255, 255));
        }
    }

    public static void applyFromHud(
        boolean hudOn,
        boolean scoreboard,
        float sbScaleV,
        int sbOpacityV,
        boolean hideScores,
        float sbX,
        float sbY,
        boolean bossBar,
        float bbScaleV,
        String bbStyleV,
        float bbX,
        float bbY,
        boolean hideName
    ) {
        if (!hudOn) {
            sbHide = false;
            bbHide = false;
        } else {
            sbHide = !scoreboard;
            sbScale = sbScaleV;
            sbOpacity = sbOpacityV;
            sbHideScores = hideScores;
            sbOffsetX = sbX;
            sbOffsetY = sbY;
            bbHide = !bossBar;
            bbScale = bbScaleV;

            bbStyle = switch (bbStyleV) {
                case "thin", "\u0422\u043e\u043d\u043a\u0438\u0439" -> 1;
                case "segmented", "\u0421\u0435\u0433\u043c\u0435\u043d\u0442\u044b" -> 2;
                case "text_only", "\u0422\u0435\u043a\u0441\u0442" -> 3;
                default -> 0;
            };
            bbOffsetX = bbX;
            bbOffsetY = bbY;
            bbHideName = hideName;
        }
    }

    @Override
    public void tick(MinecraftClient mc) {
        this.reveal.update(hudOnHint() ? 1.0F : 0.0F, 0.016666668F);
    }

    private static boolean hudOnHint() {
        return !sbHide || !bbHide;
    }

    @Override
    protected void enable() {
    }

    @Override
    protected void disable() {
        sbHide = false;
        bbHide = false;
    }

    public static void captureBossEvents(Map<UUID, ClientBossBar> events) {
        bossEvents = events;
    }

    public static Map<UUID, ClientBossBar> bossEvents() {
        return bossEvents;
    }
}
