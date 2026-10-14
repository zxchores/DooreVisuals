package dev.doorevisuals.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Soft text for full-screen menus. The text renderer of the game samples its glyph atlas without smoothing, so scaled or
 * small text looks pixelated, and NanoVG text makes full-screen menus blink. Here every string is rasterised once with
 * anti-aliasing from the Inter font file, at a resolution that follows the GUI scale, into a texture that is sampled
 * with linear filtering, and then drawn like any other texture. Strings are cached; the least recently used are freed.
 *
 * <p>If the font cannot be loaded, {@link #ready()} is false and callers fall back to the normal text renderer.</p>
 */
public final class SmoothText {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/SmoothText");
    private static final String FONT_FILE = "/assets/doorevisuals/font/inter-regular.ttf";
    private static final int CACHE_LIMIT = 96;
    private static final int PAD = 2;

    private static Font base;
    private static boolean failed;
    private static int counter;
    private static final Map<String, Entry> CACHE = new LinkedHashMap<>(128, 0.75F, true);

    private SmoothText() {
    }

    public static boolean ready() {
        return load() != null;
    }

    private static Font load() {
        if (base != null || failed) {
            return base;
        }
        try (InputStream in = SmoothText.class.getResourceAsStream(FONT_FILE)) {
            if (in == null) {
                throw new IllegalStateException("font file not found");
            }
            base = Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Throwable error) {
            failed = true;
            LOG.warn("Мягкий текст недоступен, используется обычный: {}", error.toString());
        }
        return base;
    }

    private static float density() {
        MinecraftClient mc = MinecraftClient.getInstance();
        double scale = mc != null && mc.getWindow() != null ? mc.getWindow().getScaleFactor() : 2.0;
        return (float)Math.max(2.0, Math.min(6.0, scale * 1.5));
    }

    /** Width of the string in GUI units for an em of the given size. */
    public static float width(String text, float size) {
        Entry e = entry(text, size);
        return e == null ? 0.0F : e.width;
    }

    /** Height of the line box (ascent plus descent) in GUI units. */
    public static float height(float size) {
        return size * 1.21F;
    }

    /** Draws the string with its top-left corner at (x, y), colour in ARGB. */
    public static void draw(DrawContext g, String text, float x, float y, int argb, float size) {
        Entry e = entry(text, size);
        if (e == null || (argb >>> 24) == 0) {
            return;
        }
        float k = e.density;
        g.getMatrices().pushMatrix();
        g.getMatrices().translate(x - PAD / k, y - PAD / k);
        g.getMatrices().scale(1.0F / k, 1.0F / k);
        g.drawTexture(RenderPipelines.GUI_TEXTURED, e.id, 0, 0, 0.0F, 0.0F, e.texW, e.texH, e.texW, e.texH, argb);
        g.getMatrices().popMatrix();
    }

    public static void drawCentered(DrawContext g, String text, float cx, float y, int argb, float size) {
        draw(g, text, cx - width(text, size) / 2.0F, y, argb, size);
    }

    private static Entry entry(String text, float size) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        Font font = load();
        if (font == null) {
            return null;
        }
        float k = density();
        String key = text + '\u0000' + Math.round(size * 10.0F) + '\u0000' + Math.round(k * 10.0F);
        Entry cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            Entry made = render(font, text, size, k);
            CACHE.put(key, made);
            trim();
            return made;
        } catch (Throwable error) {
            failed = true;
            LOG.warn("Мягкий текст выключен после ошибки: {}", error.toString());
            return null;
        }
    }

    private static void trim() {
        while (CACHE.size() > CACHE_LIMIT) {
            String oldest = CACHE.keySet().iterator().next();
            Entry gone = CACHE.remove(oldest);
            MinecraftClient.getInstance().getTextureManager().destroyTexture(gone.id);
        }
    }

    private static Entry render(Font base, String text, float size, float k) {
        Font font = base.deriveFont(size * k);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        hints(pg);
        pg.setFont(font);
        FontMetrics fm = pg.getFontMetrics();
        int textW = Math.max(1, fm.stringWidth(text));
        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        pg.dispose();

        int w = textW + PAD * 2;
        int h = ascent + descent + PAD * 2;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        hints(g2);
        g2.setFont(font);
        g2.setColor(Color.WHITE);
        g2.drawString(text, PAD, PAD + ascent);
        g2.dispose();

        NativeImage image = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setColorArgb(x, y, img.getRGB(x, y));
            }
        }
        Identifier id = Identifier.of("doorevisuals", "dyn/text/" + (counter++));
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new Smooth(id.toString(), image));
        return new Entry(id, w, h, k, textW / k);
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /** A dynamic texture that is sampled with linear filtering instead of the nearest-texel default. */
    private static final class Smooth extends NativeImageBackedTexture {
        Smooth(String name, NativeImage image) {
            super(() -> name, image);
            this.sampler = RenderSystem.getSamplerCache().getRepeated(FilterMode.LINEAR);
        }
    }

    private record Entry(Identifier id, int texW, int texH, float density, float width) {
    }
}
