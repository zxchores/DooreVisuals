package dev.doorevisuals.data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D.Float;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BannerBake {
    private BannerBake() {
    }

    public static void write(String kind, Path gif, Path framesDir) {
        try {
            List<BufferedImage> list = switch (kind) {
                case "pulse" -> pulse();
                case "sweep" -> sweep();
                case "mesh" -> mesh();
                default -> aurora();
            };
            GifIO.writeLoopingGif(gif, list, 7);
            GifIO.writePngSequence(new GifIO.Frames(list, delays(list.size(), 70)), framesDir);
        } catch (Exception exception) {
        }
    }

    private static int[] delays(int n, int ms) {
        int[] aint = new int[n];
        Arrays.fill(aint, ms);
        return aint;
    }

    private static List<BufferedImage> aurora() {
        List<BufferedImage> list = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            float f = i / 24.0F;
            list.add(paint(480, 160, (x, y) -> {
                float f1 = x / 480.0F;
                float f2 = y / 160.0F;
                float f3 = (float)(0.55 + 0.45 * Math.sin(f1 * 4.2 + f * Math.PI * 2.0 + f2 * 1.4));
                float f4 = (float)(0.5 + 0.5 * Math.sin(f2 * 5.1 - f * Math.PI * 2.4 + f1 * 2.2));
                float f5 = f3 * 0.65F + f4 * 0.35F;
                return mix(397327, 1335120, 4243616, 8318932, f5);
            }));
        }

        return list;
    }

    private static List<BufferedImage> pulse() {
        List<BufferedImage> list = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            float f = i / 24.0F;
            list.add(paint(480, 160, (x, y) -> {
                float f1 = 240.0F + (float)Math.sin(f * Math.PI * 2.0) * 18.0F;
                float f2 = 80.0F;
                float f3 = (float)Math.hypot(x - f1, (y - f2) * 1.7);
                float f4 = (float)Math.sin(f3 * 0.045 - f * Math.PI * 4.0);
                float f5 = (float)Math.exp(-f3 * 0.012) * (0.55F + 0.45F * f4);
                return mix(461326, 862756, 2072704, 8318932, f5);
            }));
        }

        return list;
    }

    private static List<BufferedImage> sweep() {
        List<BufferedImage> list = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            float f = i / 24.0F;
            list.add(paint(480, 160, (x, y) -> {
                float f1 = x / 480.0F;
                float f2 = 1.0F - Math.min(1.0F, Math.abs(f1 - f) * 4.2F);
                float f3 = 1.0F - Math.min(1.0F, Math.abs((x / 480.0F + y / 160.0F) * 0.5F - f) * 3.4F);
                float f4 = Math.max(f2 * 0.75F, f3 * 0.55F);
                return mix(526604, 1193262, 4243616, 13172720, f4);
            }));
        }

        return list;
    }

    private static List<BufferedImage> mesh() {
        List<BufferedImage> list = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            float f = i / 24.0F;
            BufferedImage bufferedimage = new BufferedImage(480, 160, 1);
            Graphics2D graphics2d = bufferedimage.createGraphics();
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2d.setColor(new Color(7, 10, 14));
            graphics2d.fillRect(0, 0, 480, 160);

            for (int j = 0; j < 18; j++) {
                float f1 = (j / 18.0F + f) * (float) Math.PI * 2.0F;
                float f2 = 240.0F + (float)Math.cos(f1) * 90.0F;
                float f3 = 80.0F + (float)Math.sin(f1 * 1.4) * 38.0F;
                float f4 = 16.0F + 10.0F * (0.5F + 0.5F * (float)Math.sin(f1 * 2.0F));
                graphics2d.setColor(new Color(64, 192, 160, 40 + j));
                graphics2d.fill(new Float(f2 - f4, f3 - f4, f4 * 2.0F, f4 * 2.0F));
            }

            graphics2d.setColor(new Color(126, 239, 212, 90));
            graphics2d.drawRoundRect(12, 12, 456, 136, 28, 28);
            graphics2d.dispose();
            list.add(bufferedimage);
        }

        return list;
    }

    private static BufferedImage paint(int w, int h, BannerBake.Shade shade) {
        BufferedImage bufferedimage = new BufferedImage(w, h, 1);
        int i = 2;

        for (int j = 0; j < h; j += i) {
            for (int k = 0; k < w; k += i) {
                int l = shade.at(k, j);

                for (int i1 = 0; i1 < i && j + i1 < h; i1++) {
                    for (int j1 = 0; j1 < i && k + j1 < w; j1++) {
                        bufferedimage.setRGB(k + j1, j + i1, l);
                    }
                }
            }
        }

        return bufferedimage;
    }

    private static int mix(int a, int b, int c, int d, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        if (t < 0.33F) {
            return lerp(a, b, t / 0.33F);
        } else {
            return t < 0.66F ? lerp(b, c, (t - 0.33F) / 0.33F) : lerp(c, d, (t - 0.66F) / 0.34F);
        }
    }

    private static int lerp(int a, int b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int i = a >> 16 & 0xFF;
        int j = a >> 8 & 0xFF;
        int k = a & 0xFF;
        int l = b >> 16 & 0xFF;
        int i1 = b >> 8 & 0xFF;
        int j1 = b & 0xFF;
        int k1 = (int)(i + (l - i) * t);
        int l1 = (int)(j + (i1 - j) * t);
        int i2 = (int)(k + (j1 - k) * t);
        return k1 << 16 | l1 << 8 | i2;
    }

    private interface Shade {
        int at(int var1, int var2);
    }
}
