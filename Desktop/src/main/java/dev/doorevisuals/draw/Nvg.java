package dev.doorevisuals.draw;

import com.github.noamm9.nvgrenderer.helpers.NvgText;
import com.github.noamm9.nvgrenderer.helpers.NvgText.Align;
import com.github.noamm9.nvgrenderer.nvg.Image;
import com.github.noamm9.nvgrenderer.nvg.NVG;
import com.github.noamm9.nvgrenderer.nvg.NVGPIP;
import com.github.noamm9.nvgrenderer.nvg.enums.Gradient;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.ColorHelper;

public final class Nvg {
    private static final ThreadLocal<Deque<Boolean>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Map<Integer, Color>> COLOR_CACHE = ThreadLocal.withInitial(() -> new HashMap<>(128));
    private static volatile long deadUntil;
    private static volatile int failStreak;

    private Nvg() {
    }

    public static boolean ok() {
        return System.currentTimeMillis() >= deadUntil;
    }

    public static boolean frame() {
        Deque<Boolean> deque = STACK.get();
        return !deque.isEmpty() && Boolean.TRUE.equals(deque.peek());
    }

    public static void run(DrawContext graphics, Runnable draw) {
        if (graphics != null) {
            if (!ok()) {
                draw.run();
            } else {
                try {
                    NVGPIP.drawNVG(graphics, () -> {
                        STACK.get().push(true);

                        try {
                            draw.run();
                            failStreak = 0;
                        } catch (Throwable throwable1) {
                            trip(throwable1);
                        } finally {
                            STACK.get().pop();
                        }
                    });
                } catch (Throwable throwable) {
                    trip(throwable);
                    draw.run();
                }
            }
        }
    }

    private static void trip(Throwable t) {
        failStreak++;
        long i = failStreak >= 4 ? 2500L : 350L;
        deadUntil = System.currentTimeMillis() + i;
        t.printStackTrace();
    }

    private static Color c(int argb) {
        Map<Integer, Color> map = COLOR_CACHE.get();
        Color color = map.get(argb);
        if (color != null) {
            return color;
        } else {
            Color color1 = new Color(
                ColorHelper.getRed(argb), ColorHelper.getGreen(argb), ColorHelper.getBlue(argb), Math.max(1, ColorHelper.getAlpha(argb))
            );
            if (map.size() > 256) {
                map.clear();
            }

            map.put(argb, color1);
            return color1;
        }
    }

    public static void rect(float x, float y, float w, float h, int argb, float r) {
        if (frame() && !(w <= 0.0F) && !(h <= 0.0F)) {
            NVG.INSTANCE.rect(x, y, w, h, c(argb), Math.max(0.0F, r));
        }
    }

    public static void grad(float x, float y, float w, float h, int a, int b, float r, boolean vertical) {
        if (frame() && !(w <= 0.0F) && !(h <= 0.0F)) {
            NVG.INSTANCE.gradientRect(x, y, w, h, c(a), c(b), vertical ? Gradient.TOP_BOTTOM : Gradient.LEFT_RIGHT, Math.max(0.0F, r));
        }
    }

    public static void ring(float x, float y, float w, float h, float thick, int argb, float r) {
        if (frame()) {
            NVG.INSTANCE.hollowRect(x, y, w, h, thick, c(argb), Math.max(0.0F, r));
        }
    }

    public static void shadow(float x, float y, float w, float h, float blur, float spread, float r) {
    }

    public static void circle(float x, float y, float r, int argb) {
        if (frame() && !(r <= 0.0F)) {
            NVG.INSTANCE.circle(x, y, r, c(argb));
        }
    }

    public static Image createImage(String path) {
        if (ok() && path != null && !path.isBlank()) {
            try {
                return NVG.INSTANCE.createImage(path);
            } catch (Throwable throwable) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static void image(Image image, float x, float y, float w, float h, float radius) {
        if (frame() && image != null) {
            NVG.INSTANCE.image(image, x, y, w, h, radius);
        }
    }

    public static void imageCover(Image image, float x, float y, float w, float h, float radius) {
        if (frame() && image != null) {
            NVG.INSTANCE.imageCover(image, x, y, w, h, radius);
        }
    }

    public static void deleteImage(Image image) {
        if (image != null) {
            try {
                NVG.INSTANCE.deleteImage(image);
            } catch (Throwable throwable) {
            }
        }
    }

    public static void triangle(float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
        if (frame()) {
            NVG.INSTANCE.triangle(x1, y1, x2, y2, x3, y3, c(argb));
        }
    }

    public static void line(float x1, float y1, float x2, float y2, float thick, int argb) {
        if (frame()) {
            NVG.INSTANCE.line(x1, y1, x2, y2, thick, c(argb));
        }
    }

    public static void alpha(float a) {
        if (frame()) {
            NVG.INSTANCE.globalAlpha(Math.max(0.0F, Math.min(1.0F, a)));
        }
    }

    public static void text(String s, float x, float y, int argb, float size) {
        if (frame() && s != null && !s.isEmpty()) {
            NvgText.INSTANCE.draw(s, x, y, c(argb), size, NvgText.INSTANCE.getFont(), Align.LEFT, false);
        }
    }

    public static void textCenter(String s, float cx, float y, int argb, float size) {
        if (frame() && s != null) {
            NvgText.INSTANCE.draw(s, cx, y, c(argb), size, NvgText.INSTANCE.getFont(), Align.CENTER, false);
        }
    }

    public static void textRight(String s, float rx, float y, int argb, float size) {
        if (frame() && s != null) {
            NvgText.INSTANCE.draw(s, rx, y, c(argb), size, NvgText.INSTANCE.getFont(), Align.RIGHT, false);
        }
    }

    public static void textGrad(String s, float x, float y, float size, int a, int b) {
        if (frame() && s != null && !s.isEmpty()) {
            float f = width(s, size);
            NVG.INSTANCE.textGradient(s, x, y, size, f, c(a), c(b), NvgText.INSTANCE.getFont(), Gradient.LEFT_RIGHT);
        }
    }

    public static float width(String s, float size) {
        return s != null && !s.isEmpty() ? NvgText.INSTANCE.width(s, size, NvgText.INSTANCE.getFont()) : 0.0F;
    }

    public static void push() {
        if (frame()) {
            NVG.INSTANCE.push();
        }
    }

    public static void pop() {
        if (frame()) {
            NVG.INSTANCE.pop();
        }
    }

    public static void move(float x, float y) {
        if (frame()) {
            NVG.INSTANCE.translate(x, y);
        }
    }

    public static void scale(float x, float y) {
        if (frame()) {
            NVG.INSTANCE.scale(x, y);
        }
    }

    public static void scissor(float x, float y, float w, float h) {
        if (frame()) {
            NVG.INSTANCE.pushScissor(x, y, w, h);
        }
    }

    public static void unscissor() {
        if (frame()) {
            NVG.INSTANCE.popScissor();
        }
    }
}
