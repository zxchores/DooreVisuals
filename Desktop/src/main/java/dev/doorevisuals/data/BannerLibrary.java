package dev.doorevisuals.data;

import com.github.noamm9.nvgrenderer.nvg.Image;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Nvg;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public final class BannerLibrary {
    private static final String[][] BUILTIN_GIFS = new String[][]{
        {"aurora", "Aurora", "assets/doorevisuals/textures/gui/gif/aurora.gif"},
        {"pulse", "Pulse", "assets/doorevisuals/textures/gui/gif/pulse.gif"},
        {"sweep", "Sweep", "assets/doorevisuals/textures/gui/gif/sweep.gif"},
        {"mesh", "Mesh", "assets/doorevisuals/textures/gui/gif/mesh.gif"}
    };
    private static final List<BannerLibrary.Entry> ENTRIES = new CopyOnWriteArrayList<>();
    private static final List<List<Image>> LOADED = new ArrayList<>();
    private static String selected = "builtin:aurora";
    private static boolean dirty = true;

    private BannerLibrary() {
    }

    public static Path dir() {
        return ClientPaths.banners();
    }

    public static String selectedId() {
        return selected;
    }

    public static void select(String id) {
        if (id != null && !id.isBlank()) {
            selected = id;
            GuiLayout.bannerId(id);
        }
    }

    public static void read(String id) {
        if (id != null && !id.isBlank()) {
            selected = id;
        }
    }

    public static List<BannerLibrary.Entry> entries() {
        refreshIfNeeded();
        return List.copyOf(ENTRIES);
    }

    public static void markDirty() {
        dirty = true;
    }

    public static void prepare() {
        refreshIfNeeded();
    }

    public static void reload() {
        dirty = true;
        ensureLoaded();
    }

    public static boolean importFile(Path src) {
        if (src != null && Files.isRegularFile(src)) {
            String s = src.getFileName().toString();
            String s1 = s.toLowerCase(Locale.ROOT);
            if (!s1.endsWith(".png")
                && !s1.endsWith(".gif")
                && !s1.endsWith(".svg")
                && !s1.endsWith(".jpg")
                && !s1.endsWith(".jpeg")
                && !s1.endsWith(".webp")
                && !s1.endsWith(".mp4")
                && !s1.endsWith(".webm")
                && !s1.endsWith(".mov")
                && !s1.endsWith(".mkv")) {
                return false;
            } else {
                try {
                    Files.createDirectories(dir());
                    if (!s1.endsWith(".gif") && !s1.endsWith(".mp4") && !s1.endsWith(".webm") && !s1.endsWith(".mov") && !s1.endsWith(".mkv")) {
                        Path path2 = dir().resolve(s);
                        Files.copy(src, path2, StandardCopyOption.REPLACE_EXISTING);
                        select("user:" + s);
                    } else {
                        String s2 = stem(s);
                        Path path = dir().resolve(s2 + "_frames");
                        int i = GifIO.explode(src, path);
                        Path path1 = dir().resolve(s);
                        Files.copy(src, path1, StandardCopyOption.REPLACE_EXISTING);
                        if (i > 0 && Files.isDirectory(path) && !listPngFrames(path).isEmpty()) {
                            select("userframes:" + s2);
                        } else {
                            select("user:" + s);
                        }
                    }

                    reload();
                    return true;
                } catch (Exception exception) {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static void ensureLoaded() {
        refreshIfNeeded();
        boolean flag = LOADED.size() != ENTRIES.size();
        if (!flag) {
            for (int i = 0; i < ENTRIES.size(); i++) {
                if (!ENTRIES.get(i).frames().isEmpty() && LOADED.get(i).isEmpty()) {
                    flag = true;
                    break;
                }
            }
        }

        if (flag) {
            unload();

            for (BannerLibrary.Entry bannerlibrary$entry : ENTRIES) {
                List<Image> list = new ArrayList<>();

                for (String s : bannerlibrary$entry.frames()) {
                    Image image = Nvg.createImage(s);
                    if (image != null) {
                        list.add(image);
                    }
                }

                LOADED.add(list);
            }
        }
    }

    public static void unload() {
        for (List<Image> list : LOADED) {
            for (Image image : list) {
                Nvg.deleteImage(image);
            }
        }

        LOADED.clear();
    }

    public static Image currentFrame() {
        ensureLoaded();
        if (ENTRIES.isEmpty() || LOADED.isEmpty()) {
            return null;
        } else if ("builtin:none".equals(selected)) {
            return null;
        } else if ("builtin:cycle".equals(selected)) {
            List<Integer> list = builtinGifIndexes();
            if (list.isEmpty()) {
                return null;
            } else {
                int j = list.get((int)(Anim.timeSec() / 2.4F) % list.size());
                return frameAt(j);
            }
        } else {
            int i = indexOf(selected);
            if (i < 0) {
                i = indexOf("builtin:aurora");
            }

            if (i < 0) {
                i = 0;
            }

            return frameAt(i);
        }
    }

    public static Image preview(BannerLibrary.Entry entry) {
        ensureLoaded();
        if (entry == null) {
            return null;
        } else if ("builtin:none".equals(entry.id())) {
            return null;
        } else if ("builtin:cycle".equals(entry.id())) {
            return currentFrame();
        } else {
            int i = indexOf(entry.id());
            return i < 0 ? null : frameAt(i);
        }
    }

    private static Image frameAt(int idx) {
        if (idx >= 0 && idx < ENTRIES.size() && idx < LOADED.size()) {
            BannerLibrary.Entry bannerlibrary$entry = ENTRIES.get(idx);
            List<Image> list = LOADED.get(idx);
            if (list.isEmpty()) {
                return null;
            } else if (list.size() == 1) {
                return list.get(0);
            } else {
                int[] aint = bannerlibrary$entry.delays();
                long i = 0L;

                for (int j = 0; j < list.size(); j++) {
                    i += aint[Math.min(j, aint.length - 1)];
                }

                if (i <= 0L) {
                    i = 80L * list.size();
                }

                long i1 = System.currentTimeMillis() % i;
                long k = 0L;

                for (int l = 0; l < list.size(); l++) {
                    k += aint[Math.min(l, aint.length - 1)];
                    if (i1 < k) {
                        return list.get(l);
                    }
                }

                return list.get(list.size() - 1);
            }
        } else {
            return null;
        }
    }

    private static int indexOf(String id) {
        for (int i = 0; i < ENTRIES.size(); i++) {
            if (ENTRIES.get(i).id().equals(id)) {
                return i;
            }
        }

        return -1;
    }

    private static List<Integer> builtinGifIndexes() {
        List<Integer> list = new ArrayList<>();

        for (int i = 0; i < ENTRIES.size(); i++) {
            String s = ENTRIES.get(i).id();
            if (s.startsWith("builtin:") && !s.equals("builtin:none") && !s.equals("builtin:cycle")) {
                list.add(i);
            }
        }

        return list;
    }

    private static void refreshIfNeeded() {
        if (dirty || ENTRIES.isEmpty()) {
            dirty = false;
            unload();
            ENTRIES.clear();
            ensureBuiltins();
            ENTRIES.add(new BannerLibrary.Entry("builtin:none", "\u041d\u0435\u0442", List.of(), new int[]{80}, true));
            ENTRIES.add(new BannerLibrary.Entry("builtin:cycle", "Auto", List.of(), new int[]{80}, true));

            for (String[] astring : BUILTIN_GIFS) {
                Path path = cacheDir(astring[0]);
                List<String> list = listPngFrames(path);
                int[] aint = readDelays(path, Math.max(1, list.size()));
                ENTRIES.add(new BannerLibrary.Entry("builtin:" + astring[0], astring[1], list, aint, true));
            }

            scanUserBanners();
            if (indexOf(selected) < 0) {
                String s = selected;

                selected = switch (s) {
                    case "builtin:0" -> "builtin:aurora";
                    case "builtin:1" -> "builtin:pulse";
                    case "builtin:2" -> "builtin:sweep";
                    case "builtin:3" -> "builtin:mesh";
                    case "builtin:cycle" -> "builtin:cycle";
                    default -> "builtin:aurora";
                };
            }
        }
    }

    private static void ensureBuiltins() {
        for (String[] astring : BUILTIN_GIFS) {
            Path path = cacheDir(astring[0]);
            if (listPngFrames(path).isEmpty()) {
                boolean flag = extractClasspath(astring[2], path);
                if (!flag || listPngFrames(path).isEmpty()) {
                    BannerBake.write(astring[0], path.resolveSibling(astring[0] + ".gif"), path);
                }
            }
        }
    }

    private static boolean extractClasspath(String resource, Path frames) {
        try {
            boolean flag;
            try (InputStream inputstream = BannerLibrary.class.getClassLoader().getResourceAsStream(resource)) {
                if (inputstream == null) {
                    return false;
                }

                Path path = Files.createTempFile("doore-banner-", ".gif");

                try {
                    Files.copy(inputstream, path, StandardCopyOption.REPLACE_EXISTING);
                    GifIO.explode(path, frames);
                    flag = true;
                } finally {
                    Files.deleteIfExists(path);
                }
            }

            return flag;
        } catch (Exception exception) {
            return false;
        }
    }

    private static void scanUserBanners() {
        try {
            Files.createDirectories(dir());
            List<Path> list = new ArrayList<>();

            try (Stream<Path> stream = Files.list(dir())) {
                stream.sorted()
                    .forEach(
                        p -> {
                            String s1 = p.getFileName().toString();
                            if (!s1.startsWith(".")) {
                                String s2 = s1.toLowerCase(Locale.ROOT);
                                if (Files.isRegularFile(p)
                                    && (s2.endsWith(".gif") || s2.endsWith(".mp4") || s2.endsWith(".webm") || s2.endsWith(".mov") || s2.endsWith(".mkv"))) {
                                    list.add(p);
                                }
                            }
                        }
                    );
            }

            for (Path path : list) {
                String s = stem(path.getFileName().toString());
                Path path1 = dir().resolve(s + "_frames");
                boolean flag = listPngFrames(path1).isEmpty();

                try {
                    if (!flag && Files.getLastModifiedTime(path).toMillis() > Files.getLastModifiedTime(path1).toMillis() + 500L) {
                        flag = true;
                    }
                } catch (Exception exception1) {
                }

                if (flag) {
                    try {
                        GifIO.explode(path, path1);
                    } catch (Exception exception) {
                    }
                }
            }

            try (Stream<Path> stream1 = Files.list(dir())) {
                stream1.sorted()
                    .forEach(
                        p -> {
                            String s1 = p.getFileName().toString();
                            if (!s1.startsWith(".")) {
                                String s2 = s1.toLowerCase(Locale.ROOT);
                                if (Files.isDirectory(p) && s2.endsWith("_frames")) {
                                    String s3 = s1.substring(0, s1.length() - "_frames".length());
                                    List<String> list1 = listPngFrames(p);
                                    if (!list1.isEmpty()) {
                                        ENTRIES.add(
                                            new BannerLibrary.Entry("userframes:" + s3, bannerSourceName(s3), list1, readDelays(p, list1.size()), false)
                                        );
                                    }
                                } else if (Files.isRegularFile(p)
                                    && (s2.endsWith(".png") || s2.endsWith(".svg") || s2.endsWith(".jpg") || s2.endsWith(".jpeg") || s2.endsWith(".webp"))) {
                                    ENTRIES.add(new BannerLibrary.Entry("user:" + s1, s1, List.of(p.toAbsolutePath().toString()), new int[]{80}, false));
                                }
                            }
                        }
                    );
            }
        } catch (Exception exception2) {
        }
    }

    private static String bannerSourceName(String base) {
        String[] astring = new String[]{".gif", ".mp4", ".webm", ".mov", ".mkv"};
        String s = base.toLowerCase(Locale.ROOT);

        try {
            String s1;
            try (Stream<Path> stream = Files.list(dir())) {
                s1 = stream.filter(x$0 -> Files.isRegularFile(x$0)).map(p -> p.getFileName().toString()).filter(n -> {
                    String s2 = n.toLowerCase(Locale.ROOT);
                    int i = s2.lastIndexOf(46);
                    String s3 = i < 0 ? s2 : s2.substring(0, i);
                    if (!s3.equals(s)) {
                        return false;
                    } else {
                        for (String s4 : astring) {
                            if (s2.endsWith(s4)) {
                                return true;
                            }
                        }

                        return false;
                    }
                }).findFirst().orElse(base);
            }

            return s1;
        } catch (Exception exception) {
            return base;
        }
    }

    private static Path cacheDir(String kind) {
        Path path = dir().resolve(".cache").resolve(kind);

        try {
            Files.createDirectories(path);
        } catch (Exception exception) {
        }

        return path;
    }

    private static List<String> listPngFrames(Path dir) {
        List<String> list = new ArrayList<>();
        if (dir != null && Files.isDirectory(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted()
                    .forEach(p -> list.add(p.toAbsolutePath().toString()));
            } catch (Exception exception) {
            }

            return list;
        } else {
            return list;
        }
    }

    private static int[] readDelays(Path frameDir, int count) {
        int i = Math.max(1, count);
        int[] aint = new int[i];
        Arrays.fill(aint, 80);

        try {
            Path path = frameDir.resolve("delay.txt");
            if (!Files.isRegularFile(path)) {
                return aint;
            }

            String s = Files.readString(path).trim();
            if (s.isEmpty()) {
                return aint;
            }

            String[] astring = s.split("[,\\s]+");
            if (astring.length == 1) {
                int j = Math.max(20, Integer.parseInt(astring[0]));
                Arrays.fill(aint, j);
            } else {
                for (int k = 0; k < i; k++) {
                    aint[k] = Math.max(20, Integer.parseInt(astring[Math.min(k, astring.length - 1)]));
                }
            }
        } catch (Exception exception) {
        }

        return aint;
    }

    private static String stem(String name) {
        int i = name.lastIndexOf(46);
        return i < 0 ? name : name.substring(0, i);
    }

    public record Entry(String id, String label, List<String> frames, int[] delays, boolean builtin) {
        public int delayMs() {
            return this.delays.length == 0 ? 80 : this.delays[0];
        }

        public String path() {
            return this.frames.isEmpty() ? "" : this.frames.get(0);
        }
    }
}
