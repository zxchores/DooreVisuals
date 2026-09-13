package dev.doorevisuals.cosmetics;

import dev.doorevisuals.data.ClientPaths;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class CapeFiles {
    private static final Map<String, Identifier> TEX = new LinkedHashMap<>();
    private static final List<String> LABELS = new ArrayList<>();
    private static long lastScan;

    private CapeFiles() {
    }

    public static Path dir() {
        return ClientPaths.cosmetics();
    }

    public static List<String> extraLabels() {
        scan();
        return List.copyOf(LABELS);
    }

    public static List<String> allCapeLabels() {
        scan();
        List<String> list = new ArrayList<>();
        list.add("\u041d\u0435\u0442");
        list.add("Doore");
        list.add("\u0422\u0451\u043c\u043d\u044b\u0439");
        list.add("\u0411\u0440\u0430\u0442\u044c\u044f");
        list.add("\u0421\u0432\u0430\u0434\u044c\u0431\u0430");
        list.add("\u0411\u0435\u043b\u043a\u0430");
        list.add("\u041d\u0435\u043e\u043d");
        list.addAll(LABELS);
        return list;
    }

    public static Identifier texture(String label) {
        scan();
        return TEX.get(label);
    }

    public static boolean isFileLabel(String label) {
        return label != null && TEX.containsKey(label);
    }

    public static void scan() {
        long i = System.currentTimeMillis();
        if (i - lastScan >= 1500L) {
            lastScan = i;
            Path path = dir();

            try {
                Files.createDirectories(path);
            } catch (Exception exception) {
            }

            List<Path> list = new ArrayList<>();

            try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(path, "*.png")) {
                for (Path path1 : directorystream) {
                    list.add(path1);
                }
            } catch (Exception exception1) {
            }

            list.sort(Comparator.comparing(px -> px.getFileName().toString().toLowerCase(Locale.ROOT)));
            List<String> list1 = new ArrayList<>();

            for (Path path2 : list) {
                String s = stem(path2.getFileName().toString());
                if (!s.isBlank()) {
                    String s1 = "PNG " + s;
                    list1.add(s1);
                    if (!TEX.containsKey(s1)) {
                        register(s1, path2);
                    }
                }
            }

            LABELS.clear();
            LABELS.addAll(list1);
        }
    }

    private static void register(String label, Path file) {
        try (InputStream inputstream = Files.newInputStream(file)) {
            NativeImage nativeimage = NativeImage.read(inputstream);
            NativeImageBackedTexture nativeimagebackedtexture = new NativeImageBackedTexture(() -> "doore_cape_" + slug(label), nativeimage);
            Identifier identifier = Identifier.of("doorevisuals", "dynamic/cape_png/" + slug(label));
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, nativeimagebackedtexture);
            TEX.put(label, identifier);
        } catch (Exception exception) {
        }
    }

    private static String stem(String name) {
        int i = name.lastIndexOf(46);
        String s = i > 0 ? name.substring(0, i) : name;
        return s.replace('_', ' ').trim();
    }

    private static String slug(String label) {
        return label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }
}
