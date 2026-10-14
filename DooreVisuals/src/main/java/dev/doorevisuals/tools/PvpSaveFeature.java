package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.overlay.HudTweaksFeature;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;

public final class PvpSaveFeature extends Feature implements Tick {
    private final Opt.Num window = this.opt(new Opt.Num("window", "Окно боя, сек", 8.0, 2.0, 30.0, 1.0));
    private long lastHit;

    public PvpSaveFeature() {
        super("pvp_save", "PvP Save", "Блокирует выход с паузы во время PvP", Category.TOOLS, false);
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on() && mc.player != null) {
            if (mc.player.hurtTime > 0) {
                this.lastHit = System.currentTimeMillis();
            }

            if (tagged(mc)) {
                this.lastHit = System.currentTimeMillis();
            }
        }
    }

    public static boolean blockDisconnect() {
        PvpSaveFeature pvpsavefeature = App.features().find(PvpSaveFeature.class).filter(Feature::on).orElse(null);
        if (pvpsavefeature == null) {
            return false;
        } else if (!pvpsavefeature.fighting()) {
            return false;
        } else {
            App.features().find(HudFeature.class).ifPresent(h -> h.notify("PvP", "сейчас PvP", HudFeature.NoteKind.WARN));
            return true;
        }
    }

    private boolean fighting() {
        long i = (long)(this.window.f() * 1000.0F);
        return System.currentTimeMillis() - this.lastHit < i;
    }

    private static boolean tagged(MinecraftClient mc) {
        String[] astring = new String[]{"pvp", "бой", "combat", "пвп", "в бою"};
        if (contains(board(mc), astring)) {
            return true;
        } else {
            Map<UUID, ClientBossBar> map = HudTweaksFeature.bossEvents();
            if (map != null) {
                for (ClientBossBar clientbossbar : map.values()) {
                    if (clientbossbar.getName() != null && contains(clientbossbar.getName().getString(), astring)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private static String board(MinecraftClient mc) {
        if (mc.world == null) {
            return "";
        } else {
            try {
                Scoreboard scoreboard = mc.world.getScoreboard();
                ScoreboardObjective scoreboardobjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
                if (scoreboardobjective == null) {
                    return "";
                }

                StringBuilder stringbuilder = new StringBuilder();

                for (ScoreboardEntry scoreboardentry : scoreboard.getScoreboardEntries(scoreboardobjective)) {
                    stringbuilder.append(scoreboardentry.display() != null ? scoreboardentry.display().getString() : scoreboardentry.owner())
                        .append('\n');
                }

                return stringbuilder.toString();
            } catch (Throwable throwable) {
                return "";
            }
        }
    }

    private static boolean contains(String blob, String[] needles) {
        String s = blob == null ? "" : blob.toLowerCase(Locale.ROOT);

        for (String s1 : needles) {
            if (s.contains(s1)) {
                return true;
            }
        }

        return false;
    }
}
