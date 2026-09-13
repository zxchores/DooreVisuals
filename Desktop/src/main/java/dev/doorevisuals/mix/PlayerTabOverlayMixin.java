package dev.doorevisuals.mix;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.cosmetics.CosmeticsFeature;
import dev.doorevisuals.cosmetics.CosmeticsRegistry;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.friends.FriendsFeature;
import dev.doorevisuals.tools.ThemeFeature;
import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerTabOverlayMixin {
    @Unique
    private long doore$openedAt;
    @Unique
    private final Anim doore$slide = new Anim(0.0F);

    @Inject(method = "setVisible", at = @At("HEAD"))
    private void doore$onVisible(boolean visible, CallbackInfo ci) {
        if (visible) {
            this.doore$openedAt = System.currentTimeMillis();
            this.doore$slide.snap(0.0F);
        }
    }

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void doore$badge(PlayerListEntry info, CallbackInfoReturnable<Text> cir) {
        if (App.live() && info != null && info.getProfile() != null) {
            UUID uuid = info.getProfile().id();
            boolean flag = FriendsFeature.isFriend(uuid);
            boolean flag1 = App.features().find(CosmeticsFeature.class).filter(Feature::on).map(CosmeticsFeature::showBadge).orElse(false);
            boolean flag2 = CosmeticsRegistry.isDoore(uuid);
            Text text = (Text)cir.getReturnValue();
            if (text != null) {
                String s = text.getString();
                MutableText mutabletext = text.copy();
                if (flag) {
                    mutabletext = Text.literal("\u2605 ")
                        .styled(style -> style.withColor(FriendsFeature.nickColor()))
                        .append(text.copy().styled(style -> style.withColor(FriendsFeature.nickColor())));
                } else {
                    if (!flag1 || !flag2 || s.startsWith("\u25c6")) {
                        if (!s.startsWith("\u25c6") && !s.startsWith("\u2605")) {
                            return;
                        }

                        return;
                    }

                    mutabletext = Text.literal("\u25c6 ").append(text.copy());
                }

                cir.setReturnValue(mutabletext);
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void doore$pushSlide(DrawContext g, int width, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        if (App.live()) {
            float f = (float)(System.currentTimeMillis() - this.doore$openedAt) / 220.0F;
            float f1 = Anim.easeOpen(f);
            this.doore$slide.snap(f1);
            float f2 = (1.0F - f1) * -22.0F + f1 * 28.0F;
            g.getMatrices().pushMatrix();
            g.getMatrices().translate(0.0F, f2);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void doore$popSlide(DrawContext g, int width, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        if (App.live()) {
            g.getMatrices().popMatrix();
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"), require = 0)
    private void doore$glassFill(DrawContext g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, doore$tabFill(color));
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true)
    private void doore$pingBar(DrawContext g, int width, int x, int y, PlayerListEntry info, CallbackInfo ci) {
        if (App.live() && FriendsFeature.pingBar() && info != null) {
            int i = Math.max(0, info.getLatency());
            int j = 10;
            int k = x + width - 11;
            int l = y + 1;
            g.fill(k, l, k + j, l + 6, Theme.alpha(0, 120));
            int i1 = MathHelper.clamp(j - i / 55, 1, j);
            int j1 = i < 80 ? Theme.OK : (i < 160 ? Theme.ACCENT_HOT : (i < 280 ? Theme.WARN : Theme.BAD));
            g.fill(k, l + 1, k + i1, l + 5, j1 | 0xFF000000);
            ci.cancel();
        }
    }

    @Unique
    private static int doore$tabFill(int color) {
        if (App.live() && FriendsFeature.tabGlass()) {
            int i = color >>> 24 & 0xFF;
            int j = color & 16777215;
            if (color == Integer.MIN_VALUE || i >= 112 && j < 2105376) {
                int i1 = Theme.GLASS_BG;
                int l = Math.max(150, Math.min(220, i));
                if (ThemeFeature.glassActive()) {
                    i1 = Theme.lerp(Theme.GLASS_BG, ThemeFeature.glassTint(), 0.22F * ThemeFeature.glassStrength());
                    l = Math.max(l, (int)(155.0F + 50.0F * ThemeFeature.glassStrength()));
                }

                return Theme.alpha(i1, l);
            } else if (j >= 12632256 && i <= 112) {
                int k = Math.max(16, Math.min(56, i + 8));
                return Theme.alpha(Theme.ACCENT, k);
            } else {
                return color;
            }
        } else {
            return color;
        }
    }
}
