package dev.doorevisuals.server;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.overlay.HudTweaksFeature;
import dev.doorevisuals.tools.InvClicks;
import dev.doorevisuals.tools.ServerDetect;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class EventsFeature extends Feature implements Tick {
    private static final Pattern[] PATTERNS = new Pattern[]{
        Pattern.compile("(?i)(ивент|event|анархи[яи]|anarchy)[^\\d]{0,16}(\\d{1,2}:\\d{2}|\\d+\\s*сек)?"),
        Pattern.compile("(?i)(airdrop|аирдроп|mega|мега)[^\\d]{0,16}(\\d{1,2}:\\d{2})?"),
        Pattern.compile("(?i)(босс|boss|квест)[^\\d]{0,16}(\\d{1,2}:\\d{2})?")
    };
    private final Opt.Key go = this.opt(new Opt.Key("go", "Переход", -1));
    private final Opt.Text warp = this.opt(new Opt.Text("warp", "Команда", "/anarchy"));
    private String line = "";
    private boolean goWas;

    public EventsFeature() {
        super("ft_events", "Events", "Парсинг ивентов FunTime и переход на анархию", Category.SYSTEM, false);
    }

    public String line() {
        return this.line;
    }

    public static void onChat(Text text) {
        if (text != null) {
            App.features().find(EventsFeature.class).filter(Feature::on).ifPresent(f -> f.ingest(text.getString()));
        }
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on()) {
            String s = this.scan(mc);
            if (s != null && !s.isBlank()) {
                if (!s.equals(this.line)) {
                    App.features().find(HudFeature.class).ifPresent(h -> h.islandEvent("Ивент", s));
                }

                this.line = s;
            }

            int i = (Integer)this.go.get();
            if (i > 0 && i != -1 && mc.getWindow() != null) {
                boolean flag = GLFW.glfwGetKey(mc.getWindow().getHandle(), i) == 1;
                if (flag && !this.goWas) {
                    this.travel(mc);
                }

                this.goWas = flag;
            } else {
                this.goWas = false;
            }
        } else {
            this.line = "";
        }
    }

    public void paintHud(DrawContext g) {
        if (this.on() && this.line != null && !this.line.isBlank()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            float f = minecraftclient.getWindow().getScaledWidth() * 0.5F;
            Nvg.push();
            Paint.hudPlate(g, f - 90.0F, 36.0F, 180.0F, 16.0F, Theme.ACCENT);
            Paint.textC(g, this.line, f, 40.0F, Theme.TEXT, 6.6F);
            Nvg.pop();
        }
    }

    private void travel(MinecraftClient mc) {
        if (ServerDetect.window(mc) == ServerDetect.Kind.EVENTS && mc.player != null) {
            int i = ServerDetect.findSlot(mc.player.currentScreenHandler, "анархи", "anarchy", "войти", "телепорт");
            if (i >= 0) {
                InvClicks.pickup(mc.player.currentScreenHandler.syncId, i);
                return;
            }
        }

        String s = (String)this.warp.get();
        ServerDetect.command(mc, s == null || s.isBlank() ? "/anarchy" : s);
    }

    private void ingest(String raw) {
        String s = match(raw);
        if (s != null) {
            this.line = s;
        }
    }

    private String scan(MinecraftClient mc) {
        String s = this.fromBoss();
        if (s != null) {
            return s;
        } else {
            return this.fromBoard(mc) != null ? this.fromBoard(mc) : (this.line.isBlank() ? null : this.line);
        }
    }

    private String fromBoss() {
        Map<UUID, ClientBossBar> map = HudTweaksFeature.bossEvents();
        if (map == null) {
            return null;
        } else {
            for (ClientBossBar clientbossbar : map.values()) {
                if (clientbossbar.getName() != null) {
                    String s = match(clientbossbar.getName().getString());
                    if (s != null) {
                        return s;
                    }
                }
            }

            return null;
        }
    }

    private String fromBoard(MinecraftClient mc) {
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
                    String s = scoreboardentry.display() != null ? scoreboardentry.display().getString() : scoreboardentry.owner();
                    String s1 = match(s);
                    if (s1 != null) {
                        return s1;
                    }
                }
            } catch (Throwable throwable) {
            }

            return null;
        }
    }

    private static String match(String raw) {
        if (raw == null) {
            return null;
        } else {
            String s = raw.replace('\u00a7', ' ').trim();

            for (Pattern pattern : PATTERNS) {
                Matcher matcher = pattern.matcher(s);
                if (matcher.find()) {
                    String s1 = matcher.group(1);
                    String s2 = matcher.groupCount() >= 2 ? matcher.group(2) : null;
                    return (s2 == null || s2.isBlank() ? s1 : s1 + "  " + s2).toLowerCase(Locale.ROOT);
                }
            }

            return null;
        }
    }
}
