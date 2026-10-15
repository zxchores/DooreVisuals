package dev.doorevisuals.ui;

import dev.doorevisuals.data.BannerLibrary;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pictures of the main menu that come from files: the logo, the profile picture and the frames of a video or GIF background.
 * Each is read once, scaled down to a size the menu can use, and registered as a texture. Used from the render thread only.
 */
public final class MenuTextures {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/MenuTextures");
    private static final Map<String, Pic> FILES = new HashMap<>();
    private static int counter;

    /** A loaded picture: the texture and its size in pixels. */
    public record Pic(Identifier id, int w, int h) {
    }

    // video background
    private static String videoKey = "";
    private static List<String> videoFrames = List.of();
    private static int[] videoDelays = new int[0];
    private static Pic[] videoPics = new Pic[0];
    private static int videoLoaded;
    private static long videoStarted;

    private MenuTextures() {
    }

    /** The picture of a file path (PNG or JPEG), or null when the path is empty or the file cannot be read. */
    public static Pic file(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String key = path.trim();
        Pic cached = FILES.get(key);
        if (cached != null) {
            return cached;
        }
        Pic pic = load(key, 1024);
        // A failed read is remembered too, so a bad path is not retried on every frame.
        FILES.put(key, pic == null ? new Pic(null, 0, 0) : pic);
        return pic;
    }

    private static Pic load(String path, int limit) {
        try {
            BufferedImage img = ImageIO.read(new File(path));
            if (img == null) {
                return null;
            }
            return register(img, limit);
        } catch (Exception | LinkageError error) {
            LOG.warn("Menu picture {} was not loaded: {}", path, error.toString());
            return null;
        }
    }

    private static Pic register(BufferedImage source, int limit) {
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage img = source;
        if (Math.max(w, h) > limit) {
            float k = limit / (float)Math.max(w, h);
            int nw = Math.max(1, Math.round(w * k));
            int nh = Math.max(1, Math.round(h * k));
            BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, nw, nh, null);
            g.dispose();
            img = scaled;
            w = nw;
            h = nh;
        }
        NativeImage image = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setColorArgb(x, y, img.getRGB(x, y));
            }
        }
        Identifier id = Identifier.of("doorevisuals", "dyn/menu/" + (counter++));
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(id::toString, image));
        return new Pic(id, w, h);
    }

    // ---------------------------------------------------------------- video / GIF background

    /** True when there is a video or GIF of the user that can serve as a background. */
    public static boolean hasVideo() {
        for (BannerLibrary.Entry e : BannerLibrary.entries()) {
            if (e.id().startsWith("userframes:") && !e.frames().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The current frame of the background video. The frames of the first user video or GIF are loaded one per call (so the
     * menu does not stall), at most 60 of them, at most 640 px on the long side; until all are in, the loaded ones loop.
     */
    public static Pic videoFrame() {
        if (videoKey.isEmpty()) {
            for (BannerLibrary.Entry e : BannerLibrary.entries()) {
                if (e.id().startsWith("userframes:") && !e.frames().isEmpty()) {
                    videoKey = e.id();
                    int n = Math.min(60, e.frames().size());
                    // Every step-th frame, so a long clip still covers its whole length.
                    double step = e.frames().size() / (double)n;
                    videoFrames = new java.util.ArrayList<>();
                    videoDelays = new int[n];
                    for (int i = 0; i < n; i++) {
                        int idx = Math.min(e.frames().size() - 1, (int)Math.floor(i * step));
                        videoFrames.add(e.frames().get(idx));
                        videoDelays[i] = Math.max(30, (int)Math.round(e.delays()[Math.min(idx, e.delays().length - 1)] * step));
                    }
                    videoPics = new Pic[n];
                    videoStarted = System.currentTimeMillis();
                    break;
                }
            }
            if (videoKey.isEmpty()) {
                videoKey = "-";
            }
        }
        if (videoFrames.isEmpty()) {
            return null;
        }
        if (videoLoaded < videoFrames.size()) {
            videoPics[videoLoaded] = load(videoFrames.get(videoLoaded), 640);
            videoLoaded++;
        }
        long total = 0L;
        int usable = Math.max(1, videoLoaded);
        for (int i = 0; i < usable; i++) {
            total += videoDelays[i];
        }
        long t = (System.currentTimeMillis() - videoStarted) % Math.max(1L, total);
        long acc = 0L;
        for (int i = 0; i < usable; i++) {
            acc += videoDelays[i];
            if (t < acc) {
                return videoPics[i];
            }
        }
        return videoPics[usable - 1];
    }

    /** Frees the textures of the video background when the menu closes. */
    public static void closeVideo() {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (Pic p : videoPics) {
            if (p != null && p.id() != null) {
                mc.getTextureManager().destroyTexture(p.id());
            }
        }
        videoKey = "";
        videoFrames = List.of();
        videoDelays = new int[0];
        videoPics = new Pic[0];
        videoLoaded = 0;
    }
}
