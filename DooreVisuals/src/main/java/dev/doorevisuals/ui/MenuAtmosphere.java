package dev.doorevisuals.ui;

import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import net.minecraft.client.gui.DrawContext;

/**
 * Fully procedural main-menu atmosphere.  It intentionally avoids external video/shader pipelines:
 * the background is a lightweight animated composition of gradients, a continuous stepped cloud
 * field, sun/moon and stars so the menu remains stable on Sodium/Iris setups.
 */
public final class MenuAtmosphere {
    private MenuAtmosphere() {
    }

    public static void paintBackground(DrawContext g, int w, int h) {
        float t = (System.nanoTime() % 12_000_000_000L) / 12_000_000_000.0F;
        float blend = smoothstep(0.10F, 0.78F, t);
        paintBackground(g, w, h, blend);
    }

    public static void paintBackground(DrawContext g, int w, int h, float nightBlend) {
        if (w <= 0 || h <= 0) {
            return;
        }

        float n = clamp01(nightBlend);
        float day = 1.0F - n;

        int skyTop = Theme.lerp(0x8FD0FF, 0x0A0B1A, n);
        int skyBottom = Theme.lerp(0xE8F4FF, 0x1B2141, n);
        Paint.grad(g, 0.0F, 0.0F, w, h, skyTop, skyBottom, 0.0F, true);

        float horizonY = h * 0.69F;
        int horizon = Theme.lerp(0xD8EEFF, 0x151A32, n);
        Paint.grad(g, 0.0F, horizonY - 110.0F, w, 170.0F, Theme.alpha(horizon, 0), Theme.alpha(horizon, 115), 0.0F, true);

        // Dawn/dusk atmospheric wash.
        Paint.grad(g, 0.0F, h * 0.35F, w, h * 0.40F,
            Theme.alpha(Theme.lerp(0xFFF1D9, 0x3C3F75, n), Math.round(58.0F * day + 32.0F * n)),
            Theme.alpha(Theme.lerp(0xFFFFFF, 0x0A0A18, n), 0),
            0.0F,
            true);

        thisCloud(g, w * 0.12F, h * 0.19F, 1.0F, 0.78F, n, 0.0F);
        thisCloud(g, w * 0.67F, h * 0.27F, 0.86F, 0.52F, n, 2.4F);
        thisCloud(g, w * 0.42F, h * 0.11F, 0.65F, 0.34F, n, 4.1F);

        // Sun fades out while the moon takes over the same path.
        float sunX = w * (0.20F + 0.58F * smoothstep(0.0F, 1.0F, n));
        float sunY = h * (0.22F + 0.09F * (float)Math.sin(n * Math.PI));
        float sunAlpha = day * day;
        if (sunAlpha > 0.01F) {
            Paint.circle(g, sunX, sunY, Math.max(18.0F, h * 0.045F), Theme.alpha(0xFFF4C7, Math.round(210.0F * sunAlpha)));
            Paint.circle(g, sunX, sunY, Math.max(34.0F, h * 0.082F), Theme.alpha(0xFFF4C7, Math.round(28.0F * sunAlpha)));
        }

        float moonX = w * (0.74F - 0.40F * smoothstep(0.0F, 1.0F, n));
        float moonY = h * (0.20F + 0.08F * (float)Math.sin((1.0F - n) * Math.PI));
        float moonAlpha = smoothstep(0.38F, 0.92F, n);
        if (moonAlpha > 0.01F) {
            Paint.circle(g, moonX, moonY, Math.max(17.0F, h * 0.040F), Theme.alpha(0xE8EEFF, Math.round(218.0F * moonAlpha)));
            Paint.circle(g, moonX + Math.max(5.0F, h * 0.012F), moonY - Math.max(2.5F, h * 0.006F), Math.max(15.0F, h * 0.035F), Theme.alpha(0x111426, Math.round(235.0F * moonAlpha)));
            Paint.circle(g, moonX - h * 0.013F, moonY + h * 0.010F, h * 0.0045F, Theme.alpha(0xB8C3E8, Math.round(90.0F * moonAlpha)));
            Paint.circle(g, moonX + h * 0.005F, moonY - h * 0.011F, h * 0.0035F, Theme.alpha(0xB8C3E8, Math.round(72.0F * moonAlpha)));
        }

        // Stars emerge gradually with the night.
        int starAlpha = Math.round(165.0F * moonAlpha);
        if (starAlpha > 2) {
            long frame = System.nanoTime() / 55_000_000L;
            for (int i = 0; i < 72; i++) {
                int x = Math.floorMod(i * 92821 + 113, Math.max(1, w));
                int y = Math.floorMod(i * 57113 + 29, Math.max(1, Math.round(h * 0.59F)));
                float twinkle = 0.55F + 0.45F * (float)Math.sin(frame * 0.10 + i * 1.91);
                int a = Math.round(starAlpha * twinkle);
                int s = i % 13 == 0 ? 2 : 1;
                g.fill(x, y, x + s, y + s, Theme.alpha(i % 5 == 0 ? Theme.ACCENT_HOT : 0xF3F0FF, a));
            }
        }

        // Distant horizon haze.
        Paint.grad(g, 0.0F, horizonY - 34.0F, w, 46.0F,
            Theme.alpha(Theme.lerp(0xFFFFFF, 0x6E78A8, n), Math.round(0.0F + 26.0F * day + 14.0F * n)),
            Theme.alpha(Theme.lerp(0xFFFFFF, 0x12182A, n), 0),
            0.0F,
            true);

        // Soft lower vignette and client tint.
        Paint.grad(g, 0.0F, h * 0.60F, w, h * 0.40F,
            Theme.alpha(Theme.lerp(0x0B1020, 0x070914, n), 0),
            Theme.alpha(0x05060E, 165),
            0.0F,
            true);
        g.fill(0, 0, w, h, Theme.alpha(Theme.ACCENT_DEEP, 24));
    }

    private static void thisCloud(DrawContext g, float cx, float cy, float scale, float opacity, float nightBlend, float phase) {
        float day = 1.0F - nightBlend;
        float drift = (float)Math.sin(AnimBridge.time() * 0.028F + phase) * 18.0F * scale;
        float x = cx + drift - 92.0F * scale;
        float y = cy;
        float w = 214.0F * scale;
        float h = 38.0F * scale;

        int base = Theme.alpha(Theme.lerp(0xFFFFFF, 0x7D84A4, nightBlend), Math.round((58.0F + 55.0F * day) * opacity));
        int shade = Theme.alpha(Theme.lerp(0xDDEBFF, 0x565D7D, nightBlend), Math.round((26.0F + 34.0F * day) * opacity));
        int edge = Theme.alpha(Theme.lerp(0xFFFFFF, 0xA9B0D2, nightBlend), Math.round((32.0F + 28.0F * day) * opacity));

        // One continuous stepped mass; there are no isolated cloud circles.
        Paint.box(g, x, y + h * 0.28F, w, h * 0.62F, base, h * 0.28F);
        Paint.box(g, x + 14.0F * scale, y + 2.0F * scale, 60.0F * scale, h * 0.56F, base, h * 0.24F);
        Paint.box(g, x + 54.0F * scale, y - 7.0F * scale, 86.0F * scale, h * 0.67F, base, h * 0.27F);
        Paint.box(g, x + 114.0F * scale, y + 1.0F * scale, 68.0F * scale, h * 0.57F, base, h * 0.24F);
        Paint.box(g, x + 24.0F * scale, y + h * 0.58F, 154.0F * scale, h * 0.20F, shade, h * 0.10F);
        Paint.box(g, x + 18.0F * scale, y + 1.0F * scale, 154.0F * scale, 1.4F * scale, edge, h * 0.05F);
    }

    public static void paintOverlay(DrawContext g, int w, int h, String title) {
        if (w > 0 && h > 0) {
            g.fill(0, 0, 2, h, Theme.alpha(Theme.ACCENT, 150));
            Paint.box(g, 16.0F, 14.0F, Math.min(260.0F, w * 0.40F), 42.0F, Theme.alpha(0x070914, 156), 12.0F);
            Paint.outline(g, 16.0F, 14.0F, Math.min(260.0F, w * 0.40F), 42.0F, Theme.alpha(Theme.ACCENT, 46), 12.0F);
            Paint.text(g, title, 30.0F, 24.0F, Theme.TEXT, 12.0F);
        }
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp01((x - edge0) / Math.max(0.0001F, edge1 - edge0));
        return t * t * (3.0F - 2.0F * t);
    }

    private static float clamp01(float x) {
        return Math.max(0.0F, Math.min(1.0F, x));
    }

    private static final class AnimBridge {
        private AnimBridge() {
        }

        static float time() {
            return dev.doorevisuals.draw.Anim.timeSec();
        }
    }
}
