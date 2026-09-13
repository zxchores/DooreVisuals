package dev.doorevisuals.mix;

import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.overlay.HudTweaksFeature;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarMixin {
    @Shadow
    @Final
    private Map<UUID, ClientBossBar> bossBars;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void doore$boss(DrawContext g, CallbackInfo ci) {
        HudTweaksFeature.captureBossEvents(this.bossBars);
        if (HudTweaksFeature.bossBarHidden()) {
            ci.cancel();
        } else {
            float f = HudTweaksFeature.bossBarScale();
            float f1 = HudTweaksFeature.bossBarOffsetX();
            float f2 = HudTweaksFeature.bossBarOffsetY();
            g.getMatrices().pushMatrix();
            int i = MinecraftClient.getInstance().getWindow().getScaledWidth();
            g.getMatrices().translate(f1 + i * 0.5F * (1.0F - f), f2);
            g.getMatrices().scale(f, f);
            this.paintCustom(g);
            g.getMatrices().popMatrix();
            ci.cancel();
        }
    }

    private void paintCustom(DrawContext g) {
        Nvg.run(g, () -> this.paintBars(g));
    }

    private void paintBars(DrawContext g) {
        if (this.bossBars != null && !this.bossBars.isEmpty()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            float f = minecraftclient.getWindow().getScaledWidth() * 0.5F;
            float f1 = 12.0F;
            int i = HudTweaksFeature.bossBarStyle();
            boolean flag = HudTweaksFeature.bossBarHideName();
            Nvg.push();

            for (ClientBossBar clientbossbar : this.bossBars.values()) {
                float f2 = MathHelper.clamp(clientbossbar.getPercent(), 0.0F, 1.0F);
                int j = colorOf(clientbossbar);
                String s = clientbossbar.getName() == null ? "" : clientbossbar.getName().getString();
                float f3 = i == 1 ? 120.0F : 182.0F;
                float f4 = i == 1 ? 4.0F : (i == 3 ? 0.0F : 8.0F);
                float f5 = f - f3 * 0.5F;
                float f6 = (!flag && i != 3 ? 22.0F : 14.0F) + Math.max(4.0F, f4);
                Paint.hudPlate(g, f5 - 8.0F, f1 - (!flag && i != 3 ? 14.0F : 4.0F), f3 + 16.0F, f6, j);
                if (!flag && i != 3) {
                    Paint.textC(g, s, f, f1, Theme.TEXT, 7.0F);
                    f1 += 11.0F;
                }

                if (i == 3) {
                    String s1 = flag ? Math.round(f2 * 100.0F) + "%" : s + "  " + Math.round(f2 * 100.0F) + "%";
                    Paint.textC(g, s1, f, f1, Theme.TEXT, 7.2F);
                    f1 += 14.0F;
                } else {
                    f5 = f - f3 * 0.5F;
                    if (i == 2) {
                        int k = 10;
                        float f7 = 2.0F;
                        float f8 = (f3 - f7 * (k - 1)) / k;
                        int l = Math.round(f2 * k);

                        for (int i1 = 0; i1 < k; i1++) {
                            int j1 = i1 < l ? Theme.alpha(j, 230) : Theme.alpha(16777215, 18);
                            Paint.box(g, f5 + i1 * (f8 + f7), f1, f8, f4, j1, 2.0F);
                        }
                    } else {
                        Paint.bar(g, f5, f1, f3, Math.max(4.0F, f4), f2, Theme.alpha(j, 230));
                    }

                    f1 += f4 + 10.0F;
                }
            }

            Nvg.pop();
        }
    }

    private static int colorOf(BossBar ev) {
        if (ev != null && ev.getColor() != null) {
            return switch (ev.getColor()) {
                case PINK -> -2073944;
                case BLUE -> -11886337;
                case RED -> -1946805;
                case GREEN -> -11808406;
                case YELLOW -> -1519286;
                case PURPLE -> -5154080;
                case WHITE -> -1184275;
                default -> throw new MatchException(null, null);
            };
        } else {
            return Theme.ACCENT;
        }
    }
}
