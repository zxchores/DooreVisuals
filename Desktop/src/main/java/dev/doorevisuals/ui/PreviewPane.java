package dev.doorevisuals.ui;

import dev.doorevisuals.core.Feature;
import dev.doorevisuals.cosmetics.CosmeticsFeature;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.overlay.HudTweaksFeature;
import dev.doorevisuals.tools.ViewmodelFeature;
import dev.doorevisuals.world.AtmosphereFeature;
import dev.doorevisuals.world.ChinaHatFeature;
import dev.doorevisuals.world.TargetEspFeature;
import net.minecraft.client.gui.DrawContext;

public final class PreviewPane {
    public static final float WIDTH = 280.0F;
    private static float paneX;
    private static float paneY;
    private static float paneW;
    private static float paneH;
    private static boolean yawDrag;

    private PreviewPane() {
    }

    public static boolean supports(Feature feature) {
        return feature instanceof CosmeticsFeature
            || feature instanceof ViewmodelFeature
            || feature instanceof HudFeature
            || feature instanceof HudTweaksFeature
            || feature instanceof TargetEspFeature
            || feature instanceof AtmosphereFeature
            || feature instanceof ChinaHatFeature;
    }

    public static void drawSnippet(DrawContext g, Feature feature, float x, float y, float w, float h, float s) {
        Paint.box(g, x, y, w, h, Theme.alpha(16777215, 8), 8.0F * s);
        if (feature instanceof HudFeature || feature instanceof HudTweaksFeature) {
            drawHud(g, x + 4.0F * s, y + 4.0F * s, w - 8.0F * s, h - 8.0F * s, s);
        } else if (feature instanceof TargetEspFeature targetespfeature) {
            drawEspOverlay(x, y, w, h, targetespfeature, s);
        } else if (feature instanceof AtmosphereFeature) {
            drawSky(g, x + 2.0F * s, y + 2.0F * s, w - 4.0F * s, h - 4.0F * s, s);
        }
    }

    public static void draw(
        DrawContext g, float alpha, float uiScale, float bodyX, float bodyY, float bodyW, float bodyH, Feature feature, ThemeConfigUi.Hits hits
    ) {
        if (feature != null && !(alpha < 0.01F)) {
            float f = 280.0F * uiScale;
            float f1 = (1.0F - alpha) * (f + 18.0F * uiScale);
            float f2 = bodyX + bodyW + 8.0F * uiScale + f1;
            paneX = f2;
            paneY = bodyY;
            paneW = f;
            paneH = bodyH;
            Ui.push();
            Ui.alpha(Math.max(0.0F, alpha * alpha));
            Paint.shadow(f2, bodyY, f, bodyH, 16.0F, 12.0F);
            Paint.chromeGui(g, f2, bodyY, f, bodyH, 12.0F * uiScale);
            Paint.text(g, title(feature), f2 + 14.0F * uiScale, bodyY + 12.0F * uiScale, Theme.TEXT, 9.5F * uiScale);
            Paint.text(g, subtitle(feature), f2 + 14.0F * uiScale, bodyY + 26.0F * uiScale, Theme.MUTED, 5.8F * uiScale);
            float f3 = f2 + 12.0F * uiScale;
            float f4 = bodyY + 42.0F * uiScale;
            float f5 = f - 24.0F * uiScale;
            float f6 = bodyH - 58.0F * uiScale;
            if (feature instanceof CosmeticsFeature || feature instanceof ViewmodelFeature) {
                CosmeticsDrawer.drawDummy(g, f3, f4, f5, f6);
                hits.add(f3, f4, f5, f6, PreviewPane::beginYawDrag);
                Paint.text(
                    g,
                    "\u041b\u041a\u041c \u2014 \u043a\u0440\u0443\u0442\u0438\u0442\u044c",
                    f2 + 14.0F * uiScale,
                    bodyY + bodyH - 16.0F * uiScale,
                    Theme.GHOST,
                    5.4F * uiScale
                );
            } else if (feature instanceof HudFeature || feature instanceof HudTweaksFeature) {
                drawHud(g, f3, f4, f5, f6, uiScale);
            } else if (feature instanceof TargetEspFeature targetespfeature) {
                CosmeticsDrawer.drawDummy(g, f3, f4, f5, f6);
                hits.add(f3, f4, f5, f6, PreviewPane::beginYawDrag);
                drawEspOverlay(f3, f4, f5, f6, targetespfeature, uiScale);
                Paint.text(g, targetespfeature.style(), f2 + 14.0F * uiScale, bodyY + bodyH - 16.0F * uiScale, Theme.GHOST, 5.4F * uiScale);
            } else if (feature instanceof AtmosphereFeature) {
                drawSky(g, f3, f4, f5, f6, uiScale);
            } else if (feature instanceof ChinaHatFeature chinahatfeature) {
                CosmeticsDrawer.drawDummy(g, f3, f4, f5, f6);
                hits.add(f3, f4, f5, f6, PreviewPane::beginYawDrag);
                drawHatOverlay(g, f3, f4, f5, f6, chinahatfeature, uiScale);
                Paint.text(g, chinahatfeature.style(), f2 + 14.0F * uiScale, bodyY + bodyH - 16.0F * uiScale, Theme.GHOST, 5.4F * uiScale);
            }

            Ui.pop();
        }
    }

    public static void beginYawDrag() {
        yawDrag = true;
        CosmeticsDrawer.beginYawDrag();
    }

    public static void endYawDrag() {
        yawDrag = false;
        CosmeticsDrawer.endYawDrag();
    }

    public static boolean mouseDragged(double mx, double my, double dx, double dy) {
        if (!yawDrag) {
            return false;
        } else {
            CosmeticsDrawer.beginYawDrag();
            return CosmeticsDrawer.mouseDragged(mx, my, dx, dy);
        }
    }

    public static boolean hit(double mx, double my) {
        return Paint.hit(mx, my, paneX, paneY, paneW, paneH);
    }

    private static String title(Feature feature) {
        if (feature instanceof CosmeticsFeature) {
            return "Cosmetics";
        } else if (feature instanceof ViewmodelFeature) {
            return "Viewmodel";
        } else if (feature instanceof HudFeature || feature instanceof HudTweaksFeature) {
            return "HUD";
        } else if (feature instanceof TargetEspFeature) {
            return "Target ESP";
        } else if (feature instanceof AtmosphereFeature) {
            return "Atmosphere";
        } else {
            return feature instanceof ChinaHatFeature ? "China Hat" : feature.name();
        }
    }

    private static String subtitle(Feature feature) {
        if (feature instanceof CosmeticsFeature || feature instanceof ViewmodelFeature || feature instanceof ChinaHatFeature) {
            return "\u043f\u0440\u0435\u0432\u044c\u044e \u043c\u043e\u0434\u0435\u043b\u0438";
        } else if (feature instanceof HudFeature || feature instanceof HudTweaksFeature) {
            return "\u043c\u0430\u043a\u0435\u0442 \u0438\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430";
        } else if (feature instanceof TargetEspFeature) {
            return "\u043f\u0440\u0435\u0432\u044c\u044e \u043d\u0430 \u043c\u043e\u0434\u0435\u043b\u0438";
        } else {
            return feature instanceof AtmosphereFeature ? "\u043d\u0435\u0431\u043e \u0438 \u0441\u0432\u0435\u0442" : "";
        }
    }

    private static void drawHud(DrawContext g, float x, float y, float w, float h, float s) {
        Paint.box(g, x, y, w, h, Theme.alpha(461069, 220), 8.4F * s);
        Paint.glassFast(g, x + 10.0F * s, y + 12.0F * s, 88.0F * s, 16.0F * s, 6.0F);
        Paint.text(g, "doorevisuals", x + 16.0F * s, y + 16.0F * s, Theme.TEXT, 6.2F * s);
        Paint.glassFast(g, x + 10.0F * s, y + 34.0F * s, 118.0F * s, 22.0F * s, 7.0F);
        Paint.text(g, "\u2665 20   \ud83c\udf56 18", x + 16.0F * s, y + 40.0F * s, Theme.ACCENT_HOT, 6.4F * s);
        Paint.box(g, x + 10.0F * s, y + 64.0F * s, w - 20.0F * s, 8.0F * s, Theme.alpha(Theme.ACCENT, 90), 4.0F);
        Paint.box(g, x + 10.0F * s, y + 64.0F * s, (w - 20.0F * s) * 0.62F, 8.0F * s, Theme.alpha(Theme.ACCENT_HOT, 180), 4.0F);
        Paint.text(g, "cooldown", x + 12.0F * s, y + 76.0F * s, Theme.GHOST, 5.2F * s);

        for (int i = 0; i < 3; i++) {
            float f = x + 12.0F * s + i * 28.0F * s;
            Paint.box(g, f, y + 96.0F * s, 24.0F * s, 24.0F * s, Theme.alpha(Theme.ACCENT, 28 + i * 10), 6.0F * s);
            Nvg.circle(f + 12.0F * s, y + 108.0F * s, 6.0F * s, Theme.alpha(Theme.ACCENT_HOT, 140));
        }

        Paint.text(g, "potions", x + 12.0F * s, y + 124.0F * s, Theme.GHOST, 5.2F * s);
        float f1 = x + 16.0F * s;
        float f2 = y + h - 48.0F * s;
        Paint.glassFast(g, f1, f2, w - 32.0F * s, 32.0F * s, 8.0F);
        Paint.text(g, "island  \u00b7  now playing", f1 + 10.0F * s, f2 + 10.0F * s, Theme.TEXT, 6.2F * s);
    }

    private static void drawEspOverlay(float x, float y, float w, float h, TargetEspFeature feature, float s) {
        float f = x + w * 0.5F;
        float f1 = y + h * 0.52F;
        int i = feature.tint();
        String sx = feature.style();
        float f2 = Anim.timeSec();
        if ("\u0426\u0435\u043f\u0438".equals(sx)) {
            int j1 = 8;

            for (int k1 = 0; k1 < j1; k1++) {
                float f13 = f2 * 1.4F + k1 * ((float) (Math.PI * 2) / j1);
                float f16 = f + (float)Math.cos(f13) * 28.0F * s;
                float f18 = f1 + (float)Math.sin(f13 * 1.05) * 34.0F * s;
                float f20 = 16.0F * s;
                float f22 = 8.0F * s;
                boolean flag = (k1 & 1) == 1;
                Nvg.ring(
                    f16 - (flag ? f22 : f20) * 0.5F,
                    f18 - (flag ? f20 : f22) * 0.5F,
                    flag ? f22 : f20,
                    flag ? f20 : f22,
                    2.3F * s,
                    Theme.alpha(i, 200),
                    (flag ? f22 : f22) * 0.48F
                );
            }
        } else if ("\u041f\u0440\u0438\u0437\u0440\u0430\u043a\u0438".equals(sx)) {
            for (int i1 = 0; i1 < 3; i1++) {
                float f11 = f2 * 1.35F + i1 * (float) (Math.PI * 2.0 / 3.0);

                for (int l1 = 7; l1 >= 0; l1--) {
                    float f15 = f11 - l1 * 0.18F;
                    float f17 = f + (float)Math.cos(f15) * 24.0F * s;
                    float f19 = f1 + (float)Math.sin(f15 * 1.15) * 32.0F * s;
                    float f21 = 1.0F - l1 / 8.0F;
                    float f23 = (3.2F + (l1 == 0 ? 2.4F : 0.0F)) * s * (0.45F + 0.55F * f21);
                    Nvg.circle(f17, f19, f23 * 1.55F, Theme.alpha(i, (int)(40.0F * f21)));
                    Nvg.circle(f17, f19, f23, Theme.alpha(i, (int)((l1 == 0 ? 210 : 120) * f21)));
                }
            }
        } else if ("Box".equals(sx)) {
            float f9 = 0.9F + 0.1F * (float)Math.sin(f2 * 2.2);
            float f10 = 52.0F * s * f9;
            float f12 = 58.0F * s * f9;
            float f14 = f - f10 * 0.5F;
            float f6 = f1 - f12 * 0.42F;
            float f7 = Math.min(f10, f12) * 0.34F;
            float f8 = 3.2F * s;
            int k = Theme.alpha(i, 55);
            int l = Theme.alpha(i, 220);
            drawPreviewCorners(f14, f6, f10, f12, f7, f8 * 2.2F, k);
            drawPreviewCorners(f14, f6, f10, f12, f7, f8, l);
        } else {
            for (int j = 0; j < 3; j++) {
                float f3 = f2 * 1.5F + j * (float) (Math.PI * 2.0 / 3.0);
                float f4 = f + (float)Math.cos(f3) * 22.0F * s;
                float f5 = f1 + (float)Math.sin(f3 * 1.2) * 28.0F * s;
                Nvg.circle(f4, f5, 8.5F * s, Theme.alpha(i, 50));
                Nvg.circle(f4, f5, 4.8F * s, Theme.alpha(i, 210));
            }
        }
    }

    private static void drawPreviewCorners(float x, float y, float w, float h, float len, float thick, int argb) {
        float f = thick * 0.5F;
        Nvg.rect(x, y, len, thick, argb, f);
        Nvg.rect(x, y, thick, len, argb, f);
        Nvg.rect(x + w - len, y, len, thick, argb, f);
        Nvg.rect(x + w - thick, y, thick, len, argb, f);
        Nvg.rect(x, y + h - thick, len, thick, argb, f);
        Nvg.rect(x, y + h - len, thick, len, argb, f);
        Nvg.rect(x + w - len, y + h - thick, len, thick, argb, f);
        Nvg.rect(x + w - thick, y + h - len, thick, len, argb, f);
    }

    private static void drawSky(DrawContext g, float x, float y, float w, float h, float s) {
        Paint.grad(g, x, y, w, h * 0.62F, -15392704, -30134, 8.4F * s, true);
        Paint.grad(g, x, y + h * 0.58F, w, h * 0.42F, -30134, -15069152, 0.0F, true);
        Paint.box(g, x + w * 0.72F - 28.0F * s, y + h * 0.28F - 28.0F * s, 56.0F * s, 56.0F * s, Theme.alpha(-16272, 50), 28.0F * s);
        Paint.box(g, x + w * 0.72F - 18.0F * s, y + h * 0.28F - 18.0F * s, 36.0F * s, 36.0F * s, Theme.alpha(-5984, 200), 18.0F * s);
        Paint.text(g, "небо · закат", x + 12.0F * s, y + 12.0F * s, Theme.TEXT, 6.4F * s);
    }

    private static void drawHatOverlay(DrawContext g, float x, float y, float w, float h, ChinaHatFeature hat, float s) {
        float f = x + w * 0.5F;
        float f1 = y + h * 0.16F;
        int i = hat.tint();
        String sx = hat.style();
        if ("67".equals(sx)) {
            Paint.textC(g, "6", f - 18.0F * s, f1, i, 16.0F * s);
            Paint.textC(g, "7", f + 18.0F * s, f1 + 6.0F * s, i, 16.0F * s);
        } else if ("\u0410\u043d\u0433\u0435\u043b".equals(sx)) {
            Nvg.ring(f - 22.0F * s, f1 - 4.0F * s, 44.0F * s, 12.0F * s, 2.2F * s, Theme.alpha(i, 200), 8.0F * s);
        } else {
            Nvg.circle(f, f1 + 18.0F * s, 26.0F * s, Theme.alpha(i, 40));
            Paint.box(g, f - 2.0F * s, f1 - 4.0F * s, 4.0F * s, 22.0F * s, Theme.alpha(i, 180), 2.0F * s);
            Nvg.circle(f, f1 - 4.0F * s, 4.0F * s, Theme.alpha(i, 220));
        }
    }
}
