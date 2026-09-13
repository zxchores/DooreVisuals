package dev.doorevisuals.cosmetics;

import dev.doorevisuals.data.ClientPaths;
import dev.doorevisuals.data.GifIO;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class CapeAnim {
    private static final Identifier ID = Identifier.of("doorevisuals", "dynamic/cape_anim");
    private static NativeImageBackedTexture tex;
    private static final List<NativeImage> FRAMES = new ArrayList<>();
    private static int index;
    private static long next;
    private static int delayMs = 80;
    private static boolean ready;
    private static boolean tried;

    private CapeAnim() {
    }

    public static Identifier current() {
        tick();
        return ready ? ID : null;
    }

    public static void invalidate() {
        tried = false;
        ready = false;
        index = 0;
        next = 0L;

        for (NativeImage nativeimage : FRAMES) {
            try {
                nativeimage.close();
            } catch (Throwable throwable) {
            }
        }

        FRAMES.clear();
    }

    public static void tick() {
        if (FRAMES.isEmpty() && !tried) {
            tried = true;
            load();
        }

        if (FRAMES.isEmpty()) {
            ready = false;
        } else {
            long i = System.currentTimeMillis();
            if (i >= next) {
                index = (index + 1) % FRAMES.size();
                next = i + delayMs;
                upload();
            }
        }
    }

    private static void load() {
        FRAMES.clear();
        delayMs = 80;
        Path path = ClientPaths.root().resolve("capes");

        try {
            Files.createDirectories(path);
            explodeGifs(path);
            Path path1 = newestFramesDir(path);
            List<Path> list = new ArrayList<>();
            if (path1 != null) {
                delayMs = readDelay(path1);

                try (DirectoryStream<Path> directorystream1 = Files.newDirectoryStream(path1)) {
                    for (Path path4 : directorystream1) {
                        if (path4.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png") && Files.isRegularFile(path4)) {
                            list.add(path4);
                        }
                    }
                }
            } else {
                try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(path)) {
                    for (Path path2 : directorystream) {
                        String s = path2.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (s.endsWith(".png") && Files.isRegularFile(path2)) {
                            list.add(path2);
                        }
                    }
                }
            }

            list.sort(Comparator.comparing(p -> p.getFileName().toString()));

            for (Path path3 : list) {
                try (InputStream inputstream = Files.newInputStream(path3)) {
                    NativeImage nativeimage = NativeImage.read(inputstream);
                    if (nativeimage != null && nativeimage.getWidth() > 0) {
                        FRAMES.add(nativeimage);
                    }
                } catch (Throwable throwable) {
                }
            }
        } catch (Throwable throwable3) {
        }
    }

    private static void explodeGifs(Path dir) {
        try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(dir)) {
            for (Path path : directorystream) {
                String s = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (s.endsWith(".gif") && Files.isRegularFile(path)) {
                    String s1 = path.getFileName().toString();
                    int i = s1.lastIndexOf(46);
                    if (i > 0) {
                        s1 = s1.substring(0, i);
                    }

                    Path path1 = dir.resolve(s1 + "_frames");
                    boolean flag = !Files.isDirectory(path1);
                    if (!flag) {
                        try {
                            flag = Files.getLastModifiedTime(path).toMillis() > Files.getLastModifiedTime(path1).toMillis() + 500L;
                        } catch (Exception exception1) {
                        }
                    }

                    if (flag) {
                        try {
                            GifIO.explode(path, path1);
                        } catch (Exception exception) {
                        }
                    }
                }
            }
        } catch (Exception exception2) {
        }
    }

    private static Path newestFramesDir(Path dir) {
        Path path = null;
        long i = Long.MIN_VALUE;

        try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(dir)) {
            for (Path path1 : directorystream) {
                if (Files.isDirectory(path1) && path1.getFileName().toString().toLowerCase(Locale.ROOT).endsWith("_frames")) {
                    long j = 0L;

                    try {
                        j = Files.getLastModifiedTime(path1).toMillis();
                    } catch (Exception exception) {
                    }

                    if (path == null || j >= i) {
                        path = path1;
                        i = j;
                    }
                }
            }
        } catch (Exception exception1) {
        }

        return path;
    }

    private static int readDelay(Path dir) {
        Path path = dir.resolve("delay.txt");
        if (!Files.isRegularFile(path)) {
            return 80;
        } else {
            try {
                String s = Files.readString(path);
                int i = 0;
                int j = 0;

                for (String s1 : s.split(",")) {
                    try {
                        i += Integer.parseInt(s1.trim());
                        j++;
                    } catch (NumberFormatException numberformatexception) {
                    }
                }

                return j == 0 ? 80 : Math.max(20, Math.min(500, i / j));
            } catch (Exception exception) {
                return 80;
            }
        }
    }

    private static void upload() {
        if (!FRAMES.isEmpty()) {
            NativeImage nativeimage = FRAMES.get(index);
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            minecraftclient.execute(
                () -> {
                    try {
                        if (tex == null) {
                            tex = new NativeImageBackedTexture(() -> "doore_cape_anim", clone(nativeimage));
                            minecraftclient.getTextureManager().registerTexture(ID, tex);
                        } else {
                            NativeImage nativeimage1 = tex.getImage();
                            if (nativeimage1 != null
                                && nativeimage1.getWidth() == nativeimage.getWidth()
                                && nativeimage1.getHeight() == nativeimage.getHeight()) {
                                nativeimage1.copyFrom(nativeimage);
                                tex.upload();
                            } else {
                                tex = new NativeImageBackedTexture(() -> "doore_cape_anim", clone(nativeimage));
                                minecraftclient.getTextureManager().registerTexture(ID, tex);
                            }
                        }

                        ready = true;
                    } catch (Throwable throwable) {
                        ready = false;
                    }
                }
            );
        }
    }

    private static NativeImage clone(NativeImage src) {
        NativeImage nativeimage = new NativeImage(src.getFormat(), src.getWidth(), src.getHeight(), true);
        nativeimage.copyFrom(src);
        return nativeimage;
    }
}
