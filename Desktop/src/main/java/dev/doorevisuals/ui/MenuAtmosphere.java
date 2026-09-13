package dev.doorevisuals.ui;

import dev.doorevisuals.DooreClient;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Sprites;
import dev.doorevisuals.draw.Theme;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;

public final class MenuAtmosphere {
    private MenuAtmosphere() {
    }

    public static void paintBackground(DrawContext g, int w, int h) {
        if (w > 0 && h > 0) {
            g.fill(0, 0, w, h, -16514014);

            try {
                float f = 1.7777778F;
                float f1 = (float)w / (float)h;
                int i;
                int j;
                int k;
                int l;
                if (f1 > f) {
                    i = w;
                    j = Math.round(w / f);
                    k = 0;
                    l = (h - j) / 2;
                } else {
                    j = h;
                    i = Math.round(h * f);
                    k = (w - i) / 2;
                    l = 0;
                }

                g.drawTexture(RenderPipelines.GUI_TEXTURED, Sprites.MENU_BG, k, l, 0.0F, 0.0F, i, j, i, j);
            } catch (Throwable throwable) {
            }

            g.fill(0, 0, w, h, Theme.alpha(Theme.ACCENT_DEEP, 120));
            g.fill(0, 0, Math.max(1, w * 2 / 5), h, Theme.alpha(Theme.ACCENT_DEEP, 70));
            g.fill(0, h - 64, w, h, Theme.alpha(132616, 140));
        }
    }

    public static void paintOverlay(DrawContext g, int w, int h, String title) {
        if (w > 0 && h > 0) {
            g.fill(0, 0, 3, h, Theme.alpha(Theme.ACCENT, 180));
            Paint.box(g, 14.0F, 12.0F, Math.min(260.0F, w * 0.42F), 44.0F, Theme.alpha(593939, 200), 10.0F);
            Paint.outline(g, 14.0F, 12.0F, Math.min(260.0F, w * 0.42F), 44.0F, Theme.alpha(Theme.ACCENT, 50), 10.0F);
            Paint.text(g, title, 28.0F, 22.0F, Theme.TEXT, 13.0F);
            Paint.text(g, "DOORE  ·  v" + DooreClient.VERSION, 28.0F, 40.0F, Theme.alpha(Theme.ACCENT_HOT, 200), 8.0F);
            Paint.text(g, "visual client", 18.0F, h - 20.0F, Theme.MUTED, 7.5F);
        }
    }
}
