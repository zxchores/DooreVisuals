package dev.doorevisuals.world.sky;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The surface of the moon for the sky shader: a small square texture the shader reads for the brightness of each point of the
 * disc. The alpha of a pixel tells the shader whether to use it: 0 keeps the shader's own procedural surface ("Нет"), 255
 * replaces it with the texture, which is either generated craters and seas ("Кратеры") or a picture from a file ("Файл").
 */
public final class MoonTexture {
    public static final Identifier ID = Identifier.of("doorevisuals", "dynamic/moon");
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Moon");
    private static final int SIZE = 128;

    private static NativeImageBackedTexture texture;
    private static String key = "";

    private MoonTexture() {
    }

    /** Makes sure the texture exists and holds the chosen surface; cheap to call every frame. */
    public static void update(String mode, String file) {
        String wanted = mode + "|" + file;
        if (texture != null && wanted.equals(key)) {
            return;
        }
        try {
            if (texture == null) {
                texture = new NativeImageBackedTexture(() -> "doorevisuals moon", new NativeImage(SIZE, SIZE, true));
                MinecraftClient.getInstance().getTextureManager().registerTexture(ID, texture);
            }
            key = wanted;
            NativeImage image = texture.getImage();
            int[] pixels = switch (mode) {
                case "Кратеры" -> craters();
                case "Файл" -> fromFile(file);
                default -> new int[SIZE * SIZE];
            };
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    image.setColorArgb(x, y, pixels[y * SIZE + x]);
                }
            }
            texture.upload();
        } catch (Throwable error) {
            LOG.warn("Moon texture not updated: {}", error.toString());
        }
    }

    /** Dark seas as soft blobs, craters as a shadowed floor with a bright rim, a little noise on top. */
    private static int[] craters() {
        float[] v = new float[SIZE * SIZE];
        Random r = new Random(7L);
        float[][] seas = {{0.38F, 0.36F, 0.20F}, {0.62F, 0.30F, 0.14F}, {0.55F, 0.62F, 0.18F}, {0.30F, 0.66F, 0.10F}, {0.72F, 0.55F, 0.09F}};
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                float u = x / (float)SIZE;
                float w = y / (float)SIZE;
                float b = 0.86F;
                for (float[] s : seas) {
                    float d = ((u - s[0]) * (u - s[0]) + (w - s[1]) * (w - s[1])) / (s[2] * s[2]);
                    b -= 0.34F * (float)Math.exp(-d);
                }
                v[y * SIZE + x] = b + (r.nextFloat() - 0.5F) * 0.05F;
            }
        }
        for (int i = 0; i < 80; i++) {
            float cx = r.nextFloat() * SIZE;
            float cy = r.nextFloat() * SIZE;
            float rad = 2.0F + r.nextFloat() * r.nextFloat() * 9.0F;
            for (int y = Math.max(0, (int)(cy - rad - 2)); y < Math.min(SIZE, (int)(cy + rad + 2)); y++) {
                for (int x = Math.max(0, (int)(cx - rad - 2)); x < Math.min(SIZE, (int)(cx + rad + 2)); x++) {
                    float d = (float)Math.hypot(x - cx, y - cy);
                    if (d < rad - 1.0F) {
                        v[y * SIZE + x] -= 0.10F;
                    } else if (d < rad + 0.8F) {
                        v[y * SIZE + x] += 0.12F;
                    }
                }
            }
        }
        int[] out = new int[SIZE * SIZE];
        for (int i = 0; i < out.length; i++) {
            int g = Math.max(0, Math.min(255, Math.round(v[i] * 255.0F)));
            out[i] = 0xFF000000 | (g << 16) | (g << 8) | g;
        }
        return out;
    }

    /** The middle square of the picture, scaled to the texture size; an unreadable file leaves the procedural surface. */
    private static int[] fromFile(String path) {
        int[] out = new int[SIZE * SIZE];
        try {
            if (path == null || path.isBlank()) {
                return out;
            }
            BufferedImage src = ImageIO.read(new File(path.trim()));
            if (src == null) {
                return out;
            }
            int side = Math.min(src.getWidth(), src.getHeight());
            BufferedImage square = src.getSubimage((src.getWidth() - side) / 2, (src.getHeight() - side) / 2, side, side);
            BufferedImage scaled = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(square, 0, 0, SIZE, SIZE, null);
            g.dispose();
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    out[y * SIZE + x] = scaled.getRGB(x, y) | 0xFF000000;
                }
            }
        } catch (Throwable error) {
            LOG.warn("Moon picture {} not read: {}", path, error.toString());
            return new int[SIZE * SIZE];
        }
        return out;
    }
}
