package dev.doorevisuals.server;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.overlay.HudTweaksFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class FunHelperFeature extends Feature implements Tick {
    private static final Pattern TIMER = Pattern.compile(
        "(?i)(трапк[аиуе]?|пласт[аеу]?)[^\\d]{0,12}(\\d{1,2}:\\d{2}|\\d+\\s*сек)"
    );
    private static final Zone[] ZONES = new Zone[]{
        new Zone("ловушка", "trap", 3.0, -50373),
        new Zone("слой", "layer", 4.0, Theme.ACCENT),
        new Zone("дезориентац", "disorient", 5.0, -18355),
        new Zone("toxic", "пыль", 6.0, -11808406),
        new Zone("god aura", "аура", 7.0, -8458284)
    };
    private final Opt.Flag rings = this.opt(new Opt.Flag("rings", "Зоны предмета", true));
    private final Opt.Num fallback = this.opt(new Opt.Num("radius", "Радиус пресета", 0.0, 0.0, 16.0, 0.5));
    private String trapLine = "";
    private volatile List<FunHelperFeature.Ring> draw = List.of();

    public FunHelperFeature() {
        super("fun_helper", "Fun Helper", "Таймер трапки/пласта и зоны предмета в руке", Category.SYSTEM, false);
    }

    public static String trapText() {
        return App.features()
            .find(FunHelperFeature.class)
            .filter(Feature::on)
            .map(FunHelperFeature::trapLine)
            .filter(s -> s != null && !s.isBlank())
            .orElse("");
    }

    public String trapLine() {
        return this.trapLine;
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on()) {
            this.trapLine = scanTimer(mc);
            this.draw = this.rings.get() ? this.zones(mc) : List.of();
        } else {
            this.trapLine = "";
            this.draw = List.of();
        }
    }

    public void draw(WorldRenderContext ctx) {
        for (FunHelperFeature.Ring funhelperfeature$ring : this.draw) {
            Mesh.ring(ctx, funhelperfeature$ring.x, funhelperfeature$ring.y, funhelperfeature$ring.z, funhelperfeature$ring.r, 48, funhelperfeature$ring.color);
            Mesh.disc(
                ctx,
                funhelperfeature$ring.x,
                funhelperfeature$ring.y,
                funhelperfeature$ring.z,
                Math.max(0.05, funhelperfeature$ring.r - 0.08),
                funhelperfeature$ring.r,
                40,
                Mesh.alpha(funhelperfeature$ring.color, 0.18F),
                dev.doorevisuals.draw.Types.holo()
            );
        }
    }

    private List<FunHelperFeature.Ring> zones(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            return List.of();
        } else {
            List<FunHelperFeature.Ring> list = new ArrayList<>();
            this.addHeld(list, mc.player.getMainHandStack(), mc);
            this.addHeld(list, mc.player.getOffHandStack(), mc);
            return list;
        }
    }

    private void addHeld(List<FunHelperFeature.Ring> out, ItemStack stack, MinecraftClient mc) {
        Zone zone = match(stack);
        if (zone != null) {
            Double double1 = AuctionLoreRadius(stack);
            double d0 = double1 != null ? double1 : ((Double)this.fallback.get() > 0.0 ? (Double)this.fallback.get() : zone.radius);
            float f = mc.getRenderTickCounter().getTickProgress(false);
            double d1 = MathHelper.lerp(f, mc.player.lastX, mc.player.getX());
            double d2 = MathHelper.lerp(f, mc.player.lastY, mc.player.getY());
            double d3 = MathHelper.lerp(f, mc.player.lastZ, mc.player.getZ());
            out.add(new FunHelperFeature.Ring(d1, d2 + 0.05, d3, d0, zone.color));
        }
    }

    private static Double AuctionLoreRadius(ItemStack stack) {
        return dev.doorevisuals.tools.AuctionLore.loreRadius(stack);
    }

    private static Zone match(ItemStack stack) {
        String s = dev.doorevisuals.tools.AuctionLore.blob(stack).toLowerCase(Locale.ROOT);
        if (s.isBlank()) {
            return null;
        } else {
            for (Zone zone : ZONES) {
                if (s.contains(zone.a) || s.contains(zone.b)) {
                    return zone;
                }
            }

            return null;
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
                    String s = scoreboardentry.owner();
                    if (scoreboardentry.display() != null) {
                        s = scoreboardentry.display().getString();
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
                String s1 = matcher.group(1).toLowerCase(Locale.ROOT).contains("пласт") ? "Пласт" : "Трапка";
                return s1 + "  " + matcher.group(2).trim();
            }
        }
    }

    private record Zone(String a, String b, double radius, int color) {
    }

    private record Ring(double x, double y, double z, double r, int color) {
    }
}
