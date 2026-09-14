package dev.doorevisuals.mix;

import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.overlay.HudTweaksFeature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class ScoreboardMixin {
    @Inject(
        method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void doore$scoreboardHead(DrawContext g, ScoreboardObjective objective, CallbackInfo ci) {
        if (HudTweaksFeature.scoreboardHidden()) {
            ci.cancel();
        } else {
            float f = HudTweaksFeature.scoreboardScale();
            float f1 = HudTweaksFeature.scoreboardOffsetX();
            float f2 = HudTweaksFeature.scoreboardOffsetY();
            HudTweaksFeature.sidebarBegin();
            g.getMatrices().pushMatrix();
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            int i = minecraftclient.getWindow().getScaledWidth();
            g.getMatrices().translate(f1 + i * (1.0F - f), f2);
            g.getMatrices().scale(f, f);
            Ui.frame(g, () -> paintGlass(g, objective));
            g.getMatrices().popMatrix();
            HudTweaksFeature.sidebarEnd();
            ci.cancel();
        }
    }

    private static void paintGlass(DrawContext g, ScoreboardObjective objective) {
        if (objective != null) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            Scoreboard scoreboard = objective.getScoreboard();
            String s = objective.getDisplayName() == null ? "" : objective.getDisplayName().getString();
            List<ScoreboardEntry> list = new ArrayList<>();

            try {
                for (ScoreboardEntry scoreboardentry : scoreboard.getScoreboardEntries(objective)) {
                    if (scoreboardentry != null) {
                        list.add(scoreboardentry);
                        if (list.size() >= 15) {
                            break;
                        }
                    }
                }
            } catch (Throwable throwable) {
                return;
            }

            float f5 = 7.2F;
            float f6 = 6.6F;
            float f = Paint.tw(s, f5);
            float f1 = 0.0F;
            boolean flag = HudTweaksFeature.scoreboardHideScores();
            List<String> list1 = new ArrayList<>();
            List<String> list2 = new ArrayList<>();

            for (ScoreboardEntry scoreboardentry1 : list) {
                String s1 = scoreboardentry1.owner() == null ? "" : scoreboardentry1.owner();
                if (scoreboardentry1.display() != null) {
                    s1 = scoreboardentry1.display().getString();
                }

                list1.add(s1);
                f = Math.max(f, (float)Paint.tw(s1, f6));
                String s2 = flag ? "" : String.valueOf(scoreboardentry1.value());
                list2.add(s2);
                if (!s2.isEmpty()) {
                    f1 = Math.max(f1, (float)Paint.tw(s2, f6));
                }
            }

            float f7 = 18.0F + f + (f1 > 0.0F ? 10.0F + f1 : 0.0F);
            float f8 = 16.0F + Math.max(1, list1.size()) * 11.0F;
            int j = Theme.HUD;
            int k = minecraftclient.getWindow().getScaledWidth();
            float f2 = k - f7 - 4.0F;
            float f3 = 48.0F;
            Paint.hudPlate(g, f2, f3, f7, f8, j);
            Paint.text(g, s, f2 + 10.0F, f3 + 5.0F, Theme.TEXT, f5);
            float f4 = f3 + 16.0F;

            for (int i = 0; i < list1.size(); i++) {
                Paint.text(g, list1.get(i), f2 + 10.0F, f4, Theme.alpha(16777215, 210), f6);
                if (!flag && !list2.get(i).isEmpty()) {
                    Paint.textR(g, list2.get(i), f2 + f7 - 8.0F, f4, Theme.alpha(j, 220), f6);
                }

                f4 += 11.0F;
            }
        }
    }
}
