package dev.doorevisuals.draw;

import dev.doorevisuals.tools.ThemeFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class Paint {
    public static final float GLASS_R = 12.0F;
    public static final float ACCENT_BAR = 2.4F;
    public static final float PAD = 8.0F;

    private Paint() {
    }

    /**
     * True when a shape should go to the distance field layer. That is the case inside every
     * {@link Ui#frame} block and, outside one, whenever there is no NanoVG frame to draw into — the
     * old fallback there was a hard edged {@code fill}, which the field beats on both counts.
     */
    private static boolean sdf(DrawContext g) {
        return Sdf.ok() && (Sdf.batching() || !Nvg.frame() && g != null);
    }

    public static void box(DrawContext g, float x, float y, float w, float h, int color, float r) {
        if (sdf(g)) {
            Sdf.fill(g, x, y, w, h, color, r);
        } else if (Nvg.frame()) {
            Nvg.rect(x, y, w, h, color, r);
        } else {
            g.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), color);
        }
    }

    public static void box(DrawContext g, int x, int y, int w, int h, int color, float r) {
        box(g, (float)x, (float)y, (float)w, (float)h, color, r);
    }

    public static void grad(DrawContext g, float x, float y, float w, float h, int a, int b, float r, boolean vertical) {
        if (sdf(g)) {
            Sdf.grad(g, x, y, w, h, a, b, r, vertical);
        } else if (Nvg.frame()) {
            Nvg.grad(x, y, w, h, a, b, r, vertical);
        } else {
            box(g, x, y, w, h, a, r);
        }
    }

    public static void outline(DrawContext g, float x, float y, float w, float h, int color, float r) {
        if (sdf(g)) {
            Sdf.stroke(g, x, y, w, h, color, r, 1.0F);
        } else if (Nvg.frame()) {
            Nvg.ring(x, y, w, h, 1.0F, color, r);
        } else {
            int i = Math.round(x);
            int j = Math.round(y);
            int k = Math.round(w);
            int l = Math.round(h);
            g.fill(i, j, i + k, j + 1, color);
            g.fill(i, j + l - 1, i + k, j + l, color);
            g.fill(i, j, i + 1, j + l, color);
            g.fill(i + k - 1, j, i + k, j + l, color);
        }
    }

    public static void outline(DrawContext g, int x, int y, int w, int h, int color, float r) {
        outline(g, (float)x, (float)y, (float)w, (float)h, color, r);
    }

    public static void circle(DrawContext g, float cx, float cy, float r, int color) {
        if (sdf(g)) {
            Sdf.circle(g, cx, cy, r, color);
        } else {
            Nvg.circle(cx, cy, r, color);
        }
    }

    public static void ring(DrawContext g, float cx, float cy, float r, float thickness, int color) {
        if (sdf(g)) {
            Sdf.ring(g, cx, cy, r, thickness, color);
        } else {
            Nvg.ring(cx - r, cy - r, r * 2.0F, r * 2.0F, thickness, color, r);
        }
    }

    public static void shadow(float x, float y, float w, float h, float r) {
        shadow(x, y, w, h, 12.0F, r);
    }

    /**
     * A real drop shadow: the distance field rolls the alpha off over {@code blur} pixels, so one quad
     * replaces what would otherwise be a stack of ever fainter rectangles. Without the field layer it
     * falls back to that stack, which is still better than the empty body this used to have.
     */
    public static void shadow(float x, float y, float w, float h, float blur, float r) {
        float f = Math.max(1.0F, blur);
        if (Sdf.batching()) {
            Sdf.shadow(null, x + 1.0F, y + 2.0F, w, h, Theme.alpha(0, 130), r, f);
        } else if (Nvg.frame()) {
            for (int i = 4; i >= 1; i--) {
                float f1 = f * i / 4.0F;
                Nvg.rect(x - f1 + 1.0F, y - f1 + 2.0F, w + f1 * 2.0F, h + f1 * 2.0F, Theme.alpha(0, 12), r + f1);
            }
        }
    }

    @Deprecated
    public static void chrome(DrawContext g, float x, float y, float w, float h) {
        slim(g, x, y, w, h);
    }

    public static void chromeGui(DrawContext g, float x, float y, float w, float h, float r) {
        int i = switch (Theme.CHROME) {
            case NEON -> Theme.alpha(Theme.NEON_BG, Math.round(255.0F * Theme.NAV_A));
            case MINIMAL -> Theme.alpha(Theme.MINIMAL_BG, Math.round(220.0F * Theme.NAV_A));
            case DARK -> Theme.alpha(790550, Math.round(250.0F * Theme.NAV_A));
            case VANILLA -> Theme.alpha(3421244, Math.round(235.0F * Theme.NAV_A));
            default -> Theme.alpha(1053980, Math.round(242.0F * Theme.NAV_A));
        };

        int j = switch (Theme.CHROME) {
            case NEON -> Theme.alpha(329484, Math.round(255.0F * Theme.NAV_A));
            case MINIMAL -> Theme.alpha(Theme.MINIMAL_BG, Math.round(200.0F * Theme.NAV_A));
            case DARK -> Theme.alpha(329224, Math.round(250.0F * Theme.NAV_A));
            case VANILLA -> Theme.alpha(2236968, Math.round(235.0F * Theme.NAV_A));
            default -> Theme.alpha(461069, Math.round(242.0F * Theme.NAV_A));
        };
        grad(g, x, y, w, h, i, j, r, true);
        outline(g, x, y, w, h, Theme.alpha(16777215, Theme.CHROME == Theme.Chrome.MINIMAL ? 14 : 30), r);
        if (Theme.CHROME != Theme.Chrome.MINIMAL) {
            outline(g, x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, Theme.alpha(Theme.ACCENT, 22), Math.max(1.0F, r - 1.0F));
            box(g, x + r * 0.45F, y + 1.0F, w - r * 0.9F, 1.35F, Theme.alpha(16777215, 28), 0.0F);
        }
    }

    public static void hudPlate(DrawContext g, float x, float y, float w, float h, int ac) {
        box(g, x, y, w, h, Theme.alpha(461583, 224), 10.0F);
        outline(g, x, y, w, h, Theme.alpha(16777215, 22), 10.0F);
        box(g, x, y + 6.0F, 2.4F, Math.max(8.0F, h - 12.0F), Theme.alpha(ac, 230), 1.2F);
        box(g, x + 10.0F, y + 1.1F, Math.max(0.0F, w - 20.0F), 1.0F, Theme.alpha(16777215, 16), 0.0F);
    }

    public static void scrollbar(DrawContext g, float x, float y, float h, float content, float view, float scroll) {
        if (!(content <= view + 2.0F) && !(h < 12.0F)) {
            float f = 2.6F;
            box(g, x, y, f, h, Theme.alpha(16777215, 14), 1.3F);
            float f1 = Math.max(18.0F, h * view / content);
            float f2 = Math.max(1.0F, content - view);
            float f3 = Math.max(0.0F, Math.min(1.0F, -scroll / f2));
            float f4 = y + f3 * (h - f1);
            box(g, x - 0.4F, f4, f + 0.8F, f1, Theme.alpha(Theme.ACCENT, 150), 1.6F);
        }
    }

    public static void liveBanner(DrawContext g, float x, float y, float w, float h, float r) {
        float f = Anim.timeSec();
        grad(g, x, y, w, h, Theme.alpha(Theme.ACCENT_DEEP, 255), Theme.alpha(461326, 255), r, true);
        float f1 = x + (0.12F + 0.62F * (0.5F + 0.5F * (float)Math.sin(f * 1.35))) * w;
        box(g, f1, y + 4.0F, Math.max(10.0F, w * 0.16F), h - 8.0F, Theme.alpha(Theme.ACCENT_HOT, 26), r * 0.4F);
        box(g, x + 6.0F, y + h - 10.0F, w - 12.0F, 6.0F, Theme.alpha(Theme.ACCENT, 28), 3.0F);
    }

    public static void glass(DrawContext g, float x, float y, float w, float h) {
        glass(g, x, y, w, h, 12.0F);
    }

    public static void glass(DrawContext g, float x, float y, float w, float h, float r) {
        glass(g, x, y, w, h, r, true);
    }

    public static void glassFast(DrawContext g, float x, float y, float w, float h, float r) {
        glass(g, x, y, w, h, r, false);
    }

    public static void hudBar(DrawContext g, float x, float y, float w, float h, float r) {
        box(g, x, y, w, h, Theme.alpha(658706, 220), r);
        outline(g, x, y, w, h, Theme.alpha(16777215, 48), r);
    }

    public static void glass(DrawContext g, float x, float y, float w, float h, float r, boolean shadow) {
        switch (Theme.CHROME) {
            case NEON:
                box(g, x, y, w, h, Theme.NEON_BG, r);
                outline(g, x, y, w, h, Theme.alpha(Theme.ACCENT, 120), r);
                outline(g, x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, Theme.alpha(Theme.ACCENT_HOT, 40), Math.max(1.0F, r - 1.0F));
                box(g, x, y, 2.4F, h, Theme.alpha(Theme.ACCENT, 220), 0.0F);
                box(g, x + r, y, w - r * 2.0F, 1.2F, Theme.alpha(16777215, 18), 0.0F);
                break;
            case MINIMAL:
                box(g, x, y, w, h, Theme.MINIMAL_BG, r);
                box(g, x, y, 1.6F, h, Theme.alpha(Theme.ACCENT, 160), 0.0F);
                break;
            default:
                boolean flag = ThemeFeature.glassActive();
                float f = flag ? 0.55F + 0.45F * ThemeFeature.glassStrength() : 0.5F;
                int i = Theme.GLASS_BG;
                if (flag) {
                    i = Theme.alpha(Theme.lerp(Theme.GLASS_BG, ThemeFeature.glassTint(), 0.18F), 200 + Math.round(20.0F * f));
                }

                box(g, x, y, w, h, i, r);
                outline(g, x, y, w, h, flag ? Theme.alpha(ThemeFeature.glassTint(), 40 + Math.round(30.0F * f)) : Theme.GLASS_OUTLINE, r);
                if (flag) {
                    outline(g, x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, Theme.alpha(16777215, 16), Math.max(1.0F, r - 1.0F));
                }

                box(g, x + r * 0.5F, y, w - r, 0.8F, Theme.alpha(16777215, flag ? 18 : 12), 0.0F);
        }
    }

    public static void slim(DrawContext g, float x, float y, float w, float h) {
        glassFast(g, x, y, w, h, 8.0F);
    }

    public static void keycap(DrawContext g, float x, float y, float w, float h, String label, boolean down) {
        keycap(g, x, y, w, h, label, down ? 1.0F : 0.0F, Theme.ACCENT);
    }

    public static void keycap(DrawContext g, float x, float y, float w, float h, String label, float press, int accent) {
        float f = Math.max(0.0F, Math.min(1.0F, press));
        int i = Theme.lerp(Theme.alpha(16777215, 14), accent, f);
        int j = Theme.alpha(accent, (int)(55.0F + 100.0F * f));
        float f1 = Math.max(3.5F, h * 0.32F);
        box(g, x, y, w, h, i, f1);
        outline(g, x, y, w, h, j, f1);
        int k = Theme.lerp(Theme.MUTED, -16118768, f);
        textC(g, label, x + w * 0.5F, y + h * 0.22F, k, Math.max(6.0F, h * 0.48F));
    }

    public static void panelBody(DrawContext g, float x, float y, float w, float h) {
        switch (Theme.CHROME) {
            case NEON:
                box(g, x, y, w, h, Theme.alpha(Theme.NEON_BG, 245), 9.0F);
                outline(g, x, y, w, h, Theme.alpha(Theme.ACCENT, 110), 9.0F);
                outline(g, x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, Theme.alpha(Theme.ACCENT_HOT, 36), 8.0F);
                box(g, x, y, 2.2F, h, Theme.alpha(Theme.ACCENT, 210), 0.0F);
                box(g, x + 4.0F, y, w - 8.0F, 1.0F, Theme.alpha(16777215, 14), 0.0F);
                break;
            case MINIMAL:
                box(g, x, y, w, h, Theme.alpha(Theme.MINIMAL_BG, 220), 9.0F);
                box(g, x, y, 1.5F, h, Theme.alpha(Theme.ACCENT, 170), 0.0F);
                break;
            default:
                box(g, x, y, w, h, Theme.PANEL, 9.0F);
                outline(g, x, y, w, h, Theme.GLASS_OUTLINE, 9.0F);
                box(g, x + 4.0F, y, w - 8.0F, 1.0F, Theme.alpha(16777215, 10), 0.0F);
        }
    }

    public static void check(DrawContext g, float x, float y, float s, boolean on) {
        box(g, x, y, s, s, on ? Theme.ACCENT : Theme.alpha(16777215, 18), 2.5F);
        if (on) {
            float f = s * 0.28F;
            box(g, x + f, y + f, s - f * 2.0F, s - f * 2.0F, -16119282, 1.2F);
        } else {
            outline(g, x, y, s, s, Theme.alpha(16777215, 40), 2.5F);
        }
    }

    public static void slider(DrawContext g, float x, float y, float w, float h, float p) {
        float f = Math.max(0.0F, Math.min(1.0F, p));
        grad(g, x, y + h * 0.35F, w, h * 0.3F, Theme.alpha(16777215, 18), Theme.alpha(16777215, 10), h * 0.2F, true);
        float f1 = Math.max(h, w * f);
        grad(g, x, y + h * 0.35F, f1, h * 0.3F, Theme.ACCENT, Theme.ACCENT_HOT, h * 0.2F, true);
        float f2 = x + w * f;
        circle(g, f2, y + h * 0.5F, h * 0.7F, Theme.alpha(Theme.ACCENT, 50));
        circle(g, f2, y + h * 0.5F, h * 0.55F, Theme.ACCENT);
        circle(g, f2, y + h * 0.5F, h * 0.32F, Theme.TEXT);
    }

    public static void panel(DrawContext g, int x, int y, int w, int h) {
        slim(g, x, y, w, h);
    }

    public static void text(DrawContext g, String s, float x, float y, int color, float size) {
        if (Nvg.frame()) {
            Nvg.text(s, x, y, color, size);
        } else {
            g.drawText(MinecraftClient.getInstance().textRenderer, s, Math.round(x), Math.round(y), color, false);
        }
    }

    public static void text(DrawContext g, String s, int x, int y, int color) {
        text(g, s, x, y, color, 9.0F);
    }

    public static void textC(DrawContext g, String s, float cx, float y, int color, float size) {
        if (Nvg.frame()) {
            Nvg.textCenter(s, cx, y, color, size);
        } else {
            int i = MinecraftClient.getInstance().textRenderer.getWidth(s);
            g.drawText(MinecraftClient.getInstance().textRenderer, s, Math.round(cx) - i / 2, Math.round(y), color, false);
        }
    }

    public static void textC(DrawContext g, String s, int cx, int y, int color) {
        textC(g, s, cx, y, color, 9.0F);
    }

    public static void textR(DrawContext g, String s, float rx, float y, int color, float size) {
        if (Nvg.frame()) {
            Nvg.textRight(s, rx, y, color, size);
        } else {
            int i = MinecraftClient.getInstance().textRenderer.getWidth(s);
            g.drawText(MinecraftClient.getInstance().textRenderer, s, Math.round(rx) - i, Math.round(y), color, false);
        }
    }

    public static int tw(String s) {
        return tw(s, 9.0F);
    }

    public static int tw(String s, float size) {
        return Nvg.ok() ? Math.round(Nvg.width(s, size)) : MinecraftClient.getInstance().textRenderer.getWidth(s);
    }

    public static boolean hit(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return hit(mx, my, (float)x, (float)y, (float)w, (float)h);
    }

    public static void bar(DrawContext g, float x, float y, float w, float h, float p, int fill) {
        float f = Math.max(2.2F, h * 0.5F);
        box(g, x, y, w, h, Theme.alpha(16777215, 16), f);
        float f1 = Math.max(2.0F, w * Math.max(0.0F, Math.min(1.0F, p)));
        grad(g, x, y, f1, h, fill, Theme.lerp(fill, -1, 0.28F), f, false);
        if (f1 > 6.0F) {
            box(g, x + 3.0F, y + 0.7F, Math.max(4.0F, f1 - 8.0F), Math.max(1.1F, h * 0.32F), Theme.alpha(16777215, 70), f * 0.4F);
        }
    }

    public static void heart(DrawContext g, float x, float y, float s, int color) {
        if (!sdf(g) && !Nvg.frame()) {
            text(g, "\u2665", x, y - 1.0F, color, s * 0.95F);
        } else {
            float f = Math.max(1.6F, s * 0.26F);
            circle(g, x + s * 0.3F, y + s * 0.3F, f, color);
            circle(g, x + s * 0.7F, y + s * 0.3F, f, color);
            float f1 = x + s * 0.5F;
            float f2 = y + s * 0.42F;

            for (float f3 = 0.0F; f3 <= 1.0F; f3 += 0.12F) {
                float f4 = f2 + s * 0.52F * f3;
                float f5 = s * 0.42F * (1.0F - f3);
                box(g, f1 - f5, f4, f5 * 2.0F, Math.max(1.2F, s * 0.14F), color, 0.5F);
            }
        }
    }

    public static void bar(DrawContext g, int x, int y, int w, int h, float p, int fill) {
        bar(g, (float)x, (float)y, (float)w, (float)h, p, fill);
    }

    public static void toggle(DrawContext g, float x, float y, boolean on, float anim) {
        float f = Math.max(0.0F, Math.min(1.0F, anim));
        int i = Theme.lerp(Theme.alpha(16777215, 28), Theme.ACCENT, f);
        grad(g, x, y, 34.0F, 16.0F, i, Theme.alpha(i, 180), 8.0F, true);
        float f1 = x + 2.0F + 16.0F * f;
        if (f > 0.3F) {
            circle(g, x + 26.0F, y + 8.0F, 8.0F, Theme.alpha(Theme.ACCENT_HOT, (int)(30.0F * f)));
        }

        box(g, f1, y + 2.0F, 12.0F, 12.0F, Theme.TEXT, 6.0F);
        if (f > 0.2F) {
            circle(g, x + 26.0F, y + 8.0F, 1.6F, Theme.alpha(Theme.TEXT, (int)(140.0F * f)));
        }
    }

    public static void toggle(DrawContext g, int x, int y, boolean on) {
        toggle(g, x, y, on, on ? 1.0F : 0.0F);
    }

    public static void featureRow(DrawContext g, float x, float y, float w, float h, String name, boolean on, float hover, float expand) {
        if (on) {
            grad(g, x + 2.0F, y + 1.0F, w - 4.0F, h - 2.0F, Theme.alpha(Theme.ACCENT, 38), Theme.alpha(Theme.ACCENT_DEEP, 30), 3.0F, true);
        } else if (hover > 0.01F) {
            box(g, x + 2.0F, y + 1.0F, w - 4.0F, h - 2.0F, Theme.alpha(16777215, (int)(10.0F * hover)), 3.0F);
        }

        if (hover > 0.01F) {
            box(g, x, y + 1.0F, 1.8F * hover, h - 2.0F, Theme.alpha(Theme.ACCENT, (int)(180.0F * hover)), 0.0F);
        }

        int i = on ? Theme.ACCENT_HOT : Theme.lerp(Theme.MUTED, Theme.TEXT, hover);
        text(g, name, x + 8.0F, y + 3.0F, i, 8.0F);
    }
}
