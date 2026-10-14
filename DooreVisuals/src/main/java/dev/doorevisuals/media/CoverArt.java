package dev.doorevisuals.media;

import com.github.noamm9.nvgrenderer.nvg.Image;
import dev.doorevisuals.data.ClientPaths;
import dev.doorevisuals.draw.Nvg;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.endlesssource.mediainterface.api.ArtworkDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CoverArt {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Cover");
    private static final Identifier ID = Identifier.of("doorevisuals", "dynamic/island_cover");
    private static String lastKey = "";
    private static NativeImageBackedTexture tex;
    private static Image nvgImage;
    private static boolean ready;
    private static int accent = -12533600;

    private CoverArt() {
    }

    public static int accent() {
        return accent;
    }

    public static Identifier id() {
        return ID;
    }

    public static boolean ready() {
        return ready && tex != null;
    }

    public static Image nvg() {
        return nvgImage;
    }

    public static void sync(String artworkValue) {
        String s = artworkValue == null ? "" : artworkValue;
        if (!s.equals(lastKey)) {
            lastKey = s;
            ready = false;
            if (s.isBlank()) {
                clear();
            } else {
                try {
                    Optional<byte[]> optional = ArtworkDecoder.decodeBytes(s);
                    if (optional.isEmpty() || optional.get().length < 32) {
                        clear();
                        return;
                    }

                    byte[] abyte = optional.get();
                    NativeImage nativeimage = NativeImage.read(new ByteArrayInputStream(abyte));
                    if (nativeimage == null || nativeimage.getWidth() < 1 || nativeimage.getHeight() < 1) {
                        clear();
                        return;
                    }

                    accent = sample(nativeimage);
                    MinecraftClient minecraftclient = MinecraftClient.getInstance();
                    minecraftclient.execute(() -> {
                        try {
                            clear();
                            tex = new NativeImageBackedTexture(() -> "doore_island_cover", nativeimage);
                            minecraftclient.getTextureManager().registerTexture(ID, tex);
                            tex.upload();

                            try {
                                Path path = ClientPaths.root().resolve("cache");
                                Files.createDirectories(path);
                                Path path1 = path.resolve("island_cover.png");
                                Files.write(path1, abyte);
                                nvgImage = Nvg.createImage(path1.toAbsolutePath().toString());
                            } catch (Throwable throwable1) {
                                LOG.debug("Cover NVG failed: {}", throwable1.toString());
                            }

                            ready = true;
                        } catch (Throwable throwable2) {
                            LOG.debug("Cover register failed: {}", throwable2.toString());
                            ready = false;
                        }
                    });
                } catch (Throwable throwable) {
                    LOG.debug("Cover decode failed: {}", throwable.toString());
                    clear();
                }
            }
        }
    }

    private static int sample(NativeImage img) {
        int i = img.getWidth();
        int j = img.getHeight();
        long k = 0L;
        long l = 0L;
        long i1 = 0L;
        long j1 = 0L;
        int k1 = Math.max(1, Math.min(i, j) / 12);

        for (int l1 = j / 4; l1 < j * 3 / 4; l1 += k1) {
            for (int i2 = i / 4; i2 < i * 3 / 4; i2 += k1) {
                int j2 = img.getColorArgb(i2, l1);
                int k2 = j2 >>> 24 & 0xFF;
                if (k2 >= 40) {
                    k += j2 >>> 16 & 0xFF;
                    l += j2 >>> 8 & 0xFF;
                    i1 += j2 & 0xFF;
                    j1++;
                }
            }
        }

        return j1 == 0L ? -12533600 : 0xFF000000 | (int)(k / j1) << 16 | (int)(l / j1) << 8 | (int)(i1 / j1);
    }

    private static void clear() {
        ready = false;
        if (nvgImage != null) {
            Nvg.deleteImage(nvgImage);
            nvgImage = null;
        }

        if (tex != null) {
            try {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(ID);
            } catch (Throwable throwable1) {
            }

            try {
                tex.close();
            } catch (Throwable throwable) {
            }

            tex = null;
        }
    }
}
