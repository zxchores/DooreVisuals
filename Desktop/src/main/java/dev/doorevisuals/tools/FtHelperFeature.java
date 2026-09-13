package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.overlay.HudTweaksFeature;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class FtHelperFeature extends Feature implements Tick {
    static final String[] HOSTS = new String[]{"funtime", "spookytime", "funsky"};
    private static final Pattern TIMER = Pattern.compile(
        "(?i)(\u0442\u0440\u0430\u043f\u043a[\u0430\u0438\u0443\u0435]?|\u043f\u043b\u0430\u0441\u0442[\u0430\u0435\u0443]?)[^\\d]{0,12}(\\d{1,2}:\\d{2}|\\d+\\s*\u0441\u0435\u043a)"
    );
    private final Opt.Num maxEach = this.opt(new Opt.Num("max_each", "\u041c\u0430\u043a\u0441. \u0446\u0435\u043d\u0430/\u0448\u0442", 0.0, 0.0, 1.0E7, 100.0));
    private final Opt.Key sell = this.opt(new Opt.Key("autosell", "\u0410\u0432\u0442\u043e\u0441\u0435\u043b\u043b", -1));
    private boolean sellWas;
    private boolean armed;
    private String trapLine = "";

    public FtHelperFeature() {
        super(
            "ft_helper",
            "FT Helper",
            "\u0410\u0443\u043a\u0446\u0438\u043e\u043d Funtime \u0438 \u0442\u0430\u0439\u043c\u0435\u0440 \u0442\u0440\u0430\u043f\u043a\u0438/\u043f\u043b\u0430\u0441\u0442\u0430",
            Category.SYSTEM,
            false
        );
    }

    @Override
    public void tick(MinecraftClient mc) {
        boolean flag = AuctionLore.serverMatch(mc, HOSTS);
        if (flag && !this.armed) {
            this.set(true);
            this.armed = true;
        }

        if (!flag) {
            this.armed = false;
        }

        if (!this.on()) {
            this.trapLine = "";
        } else {
            this.trapLine = scanTimer(mc);
            pollSell(mc, this.sell, this.sellWas, v -> this.sellWas = v, (Double)this.maxEach.get());
        }
    }

    public String trapLine() {
        return this.trapLine;
    }

    public static String trapText() {
        return App.features().find(FtHelperFeature.class).filter(Feature::on).map(FtHelperFeature::trapLine).filter(s -> s != null && !s.isBlank()).orElse("");
    }

    public double maxEach() {
        return (Double)this.maxEach.get();
    }

    public void paintHud(DrawContext g) {
    }

    static void pollSell(MinecraftClient mc, Opt.Key key, boolean was, Consumer<Boolean> setWas, double cap) {
        int i = (Integer)key.get();
        if (i > 0 && i != -1 && AuctionLore.auctionScreen(mc) && mc.player != null) {
            boolean flag = GLFW.glfwGetKey(mc.getWindow().getHandle(), i) == 1;
            if (flag && !was && !InvClicks.busy()) {
                autosell(mc, cap);
            }

            setWas.accept(flag);
        } else {
            setWas.accept(false);
        }
    }

    public static void autosell(MinecraftClient mc, double cap) {
        if (mc.player != null && AuctionLore.auctionScreen(mc)) {
            ScreenHandler screenhandler = mc.player.currentScreenHandler;
            double d0 = Double.MAX_VALUE;
            int i = -1;

            for (Slot slot : screenhandler.slots) {
                AuctionLore.Deal auctionlore$deal = AuctionLore.parse(slot.getStack());
                if (auctionlore$deal != null && (!(cap > 0.0) || !(auctionlore$deal.each() > cap)) && auctionlore$deal.each() < d0) {
                    d0 = auctionlore$deal.each();
                    i = slot.id;
                }
            }

            if (i >= 0) {
                InvClicks.pickup(screenhandler.syncId, i);
            }
        }
    }

    private static String scanTimer(MinecraftClient mc) {
        String s = fromScoreboard(mc);
        return s != null ? s : fromBoss(mc);
    }

    private static String fromScoreboard(MinecraftClient mc) {
        if (mc.world == null) {
            return null;
        } else {
            try {
                Scoreboard scoreboard = mc.world.getScoreboard();
                ScoreboardObjective scoreboardobjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
                if (scoreboardobjective == null) {
                    return null;
                }

                for (ScoreboardEntry scoreboardentry : scoreboard.getScoreboardEntries(scoreboardobjective)) {
                    String s = scoreboardentry.comp_2127();
                    if (scoreboardentry.comp_2129() != null) {
                        s = scoreboardentry.comp_2129().getString();
                    }

                    String s1 = matchTimer(s);
                    if (s1 != null) {
                        return s1;
                    }
                }
            } catch (Throwable throwable) {
            }

            return null;
        }
    }

    private static String fromBoss(MinecraftClient mc) {
        Map<UUID, ClientBossBar> map = HudTweaksFeature.bossEvents();
        if (map == null) {
            return null;
        } else {
            for (ClientBossBar clientbossbar : map.values()) {
                Text text = clientbossbar.getName();
                if (text != null) {
                    String s = matchTimer(text.getString());
                    if (s != null) {
                        return s;
                    }
                }
            }

            return null;
        }
    }

    private static String matchTimer(String line) {
        if (line == null) {
            return null;
        } else {
            String s = line.replace('\u00a7', ' ');
            Matcher matcher = TIMER.matcher(s);
            if (!matcher.find()) {
                return null;
            } else {
                String s1 = matcher.group(1).toLowerCase(Locale.ROOT).contains("\u043f\u043b\u0430\u0441\u0442")
                    ? "\u041f\u043b\u0430\u0441\u0442"
                    : "\u0422\u0440\u0430\u043f\u043a\u0430";
                return s1 + "  " + matcher.group(2).trim();
            }
        }
    }
}
