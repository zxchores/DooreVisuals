package dev.doorevisuals.ui;

import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Sprites;
import dev.doorevisuals.draw.Theme;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;

public final class MenuAtmosphere {
    private MenuAtmosphere() {
    }

    public static void paintBackground(DrawContext g, int w, int h) {
        boolean flag = false;

        try {
            float f = 1.7777778F;
            float f1 = (float)w / Math.max(1, h);
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
            flag = true;
        } catch (Throwable throwable) {
        }

        if (!flag) {
            g.fill(0, 0, w, h, -16513014);
            g.fill(0, 0, Math.max(1, w * 2 / 5), h, Theme.alpha(Theme.ACCENT_DEEP, 110));
        }

        g.fill(0, 0, w, h, Theme.alpha(132616, 88));
        g.fill(0, h - 56, w, h, Theme.alpha(132616, 100));
    }

    public static void paintOverlay(DrawContext g, int w, int h, String title) {
        if (w > 0 && h > 0) {
            float f = Anim.breathe(9.0F);
            g.fill(0, 0, Math.max(2, w / 5), h, Theme.alpha(Theme.ACCENT_DEEP, 70));
            g.fill(0, 0, 3, h, Theme.alpha(Theme.ACCENT, Math.round(140.0F + 60.0F * f)));
            Nvg.run(g, () -> {
                float f1 = Anim.timeSec();
                float f2 = (float)Math.sin(f1 * 0.22F);
                Nvg.circle(w * 0.22F + f2 * 22.0F, h * 0.28F, Math.max(90.0F, h * 0.28F), Theme.alpha(Theme.ACCENT, 10));
                Nvg.circle(w * 0.72F - f2 * 18.0F, h * 0.7F, Math.max(100.0F, h * 0.3F), Theme.alpha(Theme.ACCENT_DIM, 8));

                for (int i = 0; i < 10; i++) {
                    float f3 = (i * 113.0F + f1 * (3.0F + i % 4)) % Math.max(1, w);
                    float f4 = (i * 71.0F + (float)Math.sin(f1 * 0.4F + i) * 18.0F) % Math.max(1, h);
                    Nvg.circle(f3, f4, 0.8F + i % 3 * 0.35F, Theme.alpha(Theme.ACCENT_HOT, 28 + i % 4 * 8));
                }

                Paint.box(g, 14.0F, 12.0F, Math.min(260.0F, w * 0.42F), 44.0F, Theme.alpha(593939, 170), 12.0F);
                Paint.outline(g, 14.0F, 12.0F, Math.min(260.0F, w * 0.42F), 44.0F, Theme.alpha(Theme.ACCENT, 40), 12.0F);
                Paint.text(g, title, 28.0F, 22.0F, Theme.TEXT, 13.0F);
                Paint.text(g, "DOORE  \u00b7  v3.30.2", 28.0F, 40.0F, Theme.alpha(Theme.ACCENT_HOT, 200), 8.0F);
                Paint.box(g, 18.0F, h - 28.0F, Math.min(180.0F, w * 0.28F), 1.5F, Theme.alpha(Theme.ACCENT, 150), 1.0F);
                Paint.text(g, "visual client", 18.0F, h - 20.0F, Theme.MUTED, 7.5F);
            });
        }
    }
}
