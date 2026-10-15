package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.data.Folders;
import dev.doorevisuals.data.GifIO;
import dev.doorevisuals.overlay.HudFeature;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clips: the client keeps the last seconds of the game in memory (small JPEG frames) and saves them as a video on a key. The
 * clips go to the Videos folder, in a folder of their own ("DooreVisuals"). With ffmpeg installed the clip is an MP4; without
 * it, an animated GIF. Capturing and encoding run off the render thread, and a frame is skipped when the encoder is behind.
 */
public final class ClipsFeature extends Feature implements Tick {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Clips");
    private static final String LOW = "Низкое";
    private static final String MID = "Среднее";
    private static final String HIGH = "Высокое";

    private static ClipsFeature self;

    private final Opt.Num seconds = this.opt(new Opt.Num("seconds", "Сколько секунд хранить", 30.0, 10.0, 60.0, 5.0));
    private final Opt.Num fps = this.opt(new Opt.Num("fps", "Кадров в секунду", 20.0, 10.0, 30.0, 5.0));
    private final Opt.Pick quality = this.opt(new Opt.Pick("quality", "Размер кадра", MID, LOW, MID, HIGH).chips());
    private final Opt.Key saveKey = this.opt(new Opt.Key("save_key", "Клавиша: сохранить клип", -1));
    private final Opt.Flag recording = this.opt(new Opt.Flag("recording", "Хранить последние секунды", true));
    private final Opt.Action saveNow = this.opt(new Opt.Action("save_now", "Сохранить клип сейчас", ClipsFeature::saveFromMenu));
    private final Opt.Action folder = this.opt(new Opt.Action("open_folder", "Открыть папку с клипами", () -> {
        Path dir = clipsDir();
        Folders.ensure(dir);
        Folders.open(dir);
    }));

    private final ArrayDeque<byte[]> frames = new ArrayDeque<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DooreVisuals-Clips");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean busy;
    private long lastCapture;
    private boolean keyWasDown;
    private boolean saving;

    public ClipsFeature() {
        super(
                "clips",
                "Clips",
                "Последние секунды игры сохраняются в видео по клавише, в папку Видео/DooreVisuals",
                Category.TOOLS,
                false
        );
        this.badge(Badge.NEW);
        self = this;
    }

    private static Path clipsDir() {
        return Path.of(System.getProperty("user.home"), "Videos", "DooreVisuals");
    }

    private static void saveFromMenu() {
        ClipsFeature f = self;
        if (f != null) {
            f.save();
        }
    }

    @Override
    public void tick(MinecraftClient mc) {
        int key = this.saveKey.get();
        if (key > 0 && mc.currentScreen == null) {
            boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
            if (down && !this.keyWasDown) {
                this.save();
            }
            this.keyWasDown = down;
        }
        if (!this.on() || !this.recording.get()) {
            synchronized (this.frames) {
                this.frames.clear();
            }
        }
    }

    /** Called at the end of every rendered frame (see GameFrameMixin): takes a small picture at the chosen frame rate. */
    public static void frame() {
        ClipsFeature f = self;
        if (f != null) {
            f.capture(MinecraftClient.getInstance());
        }
    }

    private void capture(MinecraftClient mc) {
        if (!this.on() || !this.recording.get() || mc.world == null || mc.player == null || this.busy || this.saving) {
            return;
        }
        long now = System.nanoTime();
        if (now - this.lastCapture < 1_000_000_000L / Math.max(1, this.fps.i())) {
            return;
        }
        this.lastCapture = now;
        this.busy = true;
        int factor = switch (this.quality.get()) {
            case LOW -> 4;
            case HIGH -> 2;
            default -> 3;
        };
        int max = Math.max(1, Math.round(this.seconds.f() * this.fps.f()));
        try {
            ScreenshotRecorder.takeScreenshot(mc.getFramebuffer(), factor, image -> {
                int w = image.getWidth();
                int h = image.getHeight();
                int[] pixels = image.copyPixelsArgb();
                image.close();
                this.worker.execute(() -> this.encode(pixels, w, h, max));
            });
        } catch (Throwable error) {
            this.busy = false;
            LOG.warn("Clip frame was not captured: {}", error.toString());
        }
    }

    /** Off the render thread: the pixels become a JPEG and join the ring of frames, the oldest one leaves. */
    private void encode(int[] pixels, int w, int h, int max) {
        try {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, w, h, pixels, 0, w);
            ByteArrayOutputStream out = new ByteArrayOutputStream(32 * 1024);
            ImageIO.write(img, "jpg", out);
            synchronized (this.frames) {
                this.frames.addLast(out.toByteArray());
                while (this.frames.size() > max) {
                    this.frames.removeFirst();
                }
            }
        } catch (Throwable error) {
            LOG.warn("Clip frame was not encoded: {}", error.toString());
        } finally {
            this.busy = false;
        }
    }

    /** Saves the frames that are in the buffer as a video; the write runs on the worker, the result comes back as a notification. */
    public void save() {
        List<byte[]> copy;
        synchronized (this.frames) {
            copy = new ArrayList<>(this.frames);
        }
        if (copy.size() < 10) {
            this.say("Клип", "Пока нечего сохранять: включите хранение и поиграйте несколько секунд");
            return;
        }
        if (this.saving) {
            return;
        }
        this.saving = true;
        int rate = Math.max(1, this.fps.i());
        this.worker.execute(() -> {
            try {
                Path file = write(copy, rate);
                this.say("Клип сохранён", file.getFileName().toString());
            } catch (Throwable error) {
                LOG.warn("Clip was not saved: {}", error.toString());
                this.say("Клип", "Не удалось сохранить: " + error.getMessage());
            } finally {
                this.saving = false;
            }
        });
    }

    private void say(String title, String body) {
        MinecraftClient.getInstance().execute(() -> App.features().find(HudFeature.class).ifPresent(h -> h.notify(title, body)));
    }

    private static Path write(List<byte[]> jpgs, int rate) throws Exception {
        Path dir = clipsDir();
        Files.createDirectories(dir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String ffmpeg = GifIO.ffmpeg();
        if (ffmpeg != null) {
            Path temp = Files.createTempDirectory("doore-clip-");
            try {
                for (int i = 0; i < jpgs.size(); i++) {
                    Files.write(temp.resolve(String.format("%05d.jpg", i)), jpgs.get(i));
                }
                Path out = dir.resolve("clip-" + stamp + ".mp4");
                Process p = new ProcessBuilder(ffmpeg, "-y", "-loglevel", "error", "-framerate", String.valueOf(rate),
                        "-i", temp.resolve("%05d.jpg").toString(), "-c:v", "libx264", "-preset", "veryfast", "-crf", "23",
                        "-pix_fmt", "yuv420p", "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2", out.toString())
                        .redirectErrorStream(true).start();
                p.getInputStream().readAllBytes();
                if (!p.waitFor(180, TimeUnit.SECONDS) || p.exitValue() != 0 || !Files.exists(out)) {
                    throw new IllegalStateException("ffmpeg не смог собрать видео");
                }
                return out;
            } finally {
                try (var stream = Files.list(temp)) {
                    stream.forEach(f -> f.toFile().delete());
                }
                Files.deleteIfExists(temp);
            }
        }
        // No ffmpeg: an animated GIF, at most 12 frames per second and 480 pixels wide.
        Path out = dir.resolve("clip-" + stamp + ".gif");
        writeGif(jpgs, rate, out.toFile());
        return out;
    }

    private static void writeGif(List<byte[]> jpgs, int rate, File file) throws Exception {
        int step = Math.max(1, (int)Math.ceil(rate / 12.0));
        int delay = Math.max(2, Math.round(100.0F * step / rate));
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        IIOMetadata meta = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB), param);
        String format = meta.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode)meta.getAsTree(format);
        IIOMetadataNode gce = node(root, "GraphicControlExtension");
        gce.setAttribute("disposalMethod", "none");
        gce.setAttribute("userInputFlag", "FALSE");
        gce.setAttribute("transparentColorFlag", "FALSE");
        gce.setAttribute("delayTime", String.valueOf(delay));
        gce.setAttribute("transparentColorIndex", "0");
        IIOMetadataNode apps = node(root, "ApplicationExtensions");
        IIOMetadataNode loop = new IIOMetadataNode("ApplicationExtension");
        loop.setAttribute("applicationID", "NETSCAPE");
        loop.setAttribute("authenticationCode", "2.0");
        loop.setUserObject(new byte[] {1, 0, 0});
        apps.appendChild(loop);
        meta.setFromTree(format, root);
        try (ImageOutputStream out = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(out);
            writer.prepareWriteSequence(null);
            for (int i = 0; i < jpgs.size(); i += step) {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(jpgs.get(i)));
                if (img.getWidth() > 480) {
                    int nh = Math.max(1, Math.round(img.getHeight() * 480.0F / img.getWidth()));
                    BufferedImage small = new BufferedImage(480, nh, BufferedImage.TYPE_INT_RGB);
                    var g = small.createGraphics();
                    g.drawImage(img.getScaledInstance(480, nh, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
                    g.dispose();
                    img = small;
                }
                writer.writeToSequence(new IIOImage(img, null, meta), param);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private static IIOMetadataNode node(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
                return (IIOMetadataNode)root.item(i);
            }
        }
        IIOMetadataNode created = new IIOMetadataNode(name);
        root.appendChild(created);
        return created;
    }
}
