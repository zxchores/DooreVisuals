package dev.doorevisuals.data;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class GifIO {
    private static final int MAX_FRAMES = 120;
    private static final int MIN_DELAY = 20;
    private static final int MAX_DELAY = 5000;
    private static String cachedFfmpeg;
    private static String cachedFfprobe;
    private static boolean lookedUpBins;

    private GifIO() {
    }

    private static ImageReader pickGifReader(Iterator<ImageReader> readers) {
        ImageReader imagereader = readers.next();
        ImageReader imagereader1 = imagereader;
        String s = imagereader.getClass().getName();
        if (!s.contains("twelvemonkeys") && !s.contains("TwelveMonkeys")) {
            while (readers.hasNext()) {
                ImageReader imagereader2 = readers.next();
                String s1 = imagereader2.getClass().getName();
                if (s1.contains("twelvemonkeys") || s1.contains("TwelveMonkeys")) {
                    imagereader1 = imagereader2;
                    break;
                }
            }

            return imagereader1;
        } else {
            return imagereader;
        }
    }

    private static GifIO.Frames ffmpegRead(Path gif) {
        String s = findBin("ffmpeg");
        if (s != null && gif != null && Files.isRegularFile(gif)) {
            Path path = null;

            Object object;
            try {
                path = Files.createTempDirectory("doore-gif-");
                ArrayList<String> arraylist = new ArrayList<>();
                arraylist.add(s);
                arraylist.add("-hide_banner");
                arraylist.add("-loglevel");
                arraylist.add("error");
                arraylist.add("-y");
                arraylist.add("-i");
                arraylist.add(gif.toAbsolutePath().toString());
                if (isVideo(gif)) {
                    arraylist.add("-vf");
                    arraylist.add("fps=12");
                }

                arraylist.add("-start_number");
                arraylist.add("0");
                arraylist.add(path.resolve("frame_%04d.png").toString());
                ProcessBuilder processbuilder = new ProcessBuilder(arraylist);
                processbuilder.redirectErrorStream(true);
                Process process = processbuilder.start();
                drain(process);
                if (!process.waitFor(45L, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    return null;
                }

                if (process.exitValue() != 0) {
                    return null;
                }

                List<Path> list = new ArrayList<>();

                try (Stream<Path> stream = Files.list(path)) {
                    stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .forEach(list::add);
                }

                if (list.isEmpty()) {
                    return null;
                }

                int i1 = list.size() > 120 ? (int)Math.ceil(list.size() / 120.0) : 1;
                int[] aint = ffprobeDelays(gif, list.size());
                List<BufferedImage> list1 = new ArrayList<>();
                List<Integer> list2 = new ArrayList<>();

                for (int i = 0; i < list.size(); i += i1) {
                    BufferedImage bufferedimage = ImageIO.read(list.get(i).toFile());
                    if (bufferedimage != null) {
                        list1.add(bufferedimage);
                        int j = 0;
                        int k = 0;

                        for (int l = i; l < Math.min(list.size(), i + i1); l++) {
                            j += aint[Math.min(l, aint.length - 1)];
                            k++;
                        }

                        list2.add(Math.max(20, Math.min(5000, k == 0 ? 80 : j)));
                    }
                }

                if (!list1.isEmpty()) {
                    int[] aint1 = new int[list2.size()];

                    for (int j1 = 0; j1 < aint1.length; j1++) {
                        aint1[j1] = list2.get(j1);
                    }

                    return new GifIO.Frames(list1, aint1);
                }

                object = null;
            } catch (Exception exception1) {
                return null;
            } finally {
                if (path != null) {
                    try (Stream<Path> stream1 = Files.walk(path)) {
                        stream1.sorted(Comparator.reverseOrder()).forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ioexception) {
                            }
                        });
                    } catch (Exception exception) {
                    }
                }
            }

            return (GifIO.Frames)object;
        } else {
            return null;
        }
    }

    private static int[] ffprobeDelays(Path gif, int count) {
        int[] aint = new int[Math.max(1, count)];
        Arrays.fill(aint, 80);
        String s = findBin("ffprobe");
        if (s == null) {
            return aint;
        } else {
            try {
                ProcessBuilder processbuilder = new ProcessBuilder(
                    s, "-v", "error", "-select_streams", "v:0", "-show_entries", "frame=duration_time", "-of", "csv=p=0", gif.toAbsolutePath().toString()
                );
                processbuilder.redirectErrorStream(true);
                Process process = processbuilder.start();
                List<String> list = new ArrayList<>();
                BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

                String s1;
                try {
                    while ((s1 = bufferedreader.readLine()) != null) {
                        if (!s1.isBlank()) {
                            list.add(s1.trim());
                        }
                    }
                } catch (Throwable throwable1) {
                    try {
                        bufferedreader.close();
                    } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                    }

                    throw throwable1;
                }

                bufferedreader.close();
                if (!process.waitFor(12L, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    return aint;
                }

                int j = 0;

                for (String s2 : list) {
                    if (j >= aint.length) {
                        break;
                    }

                    try {
                        double d0 = Double.parseDouble(s2.split("[,\\s]+")[0]);
                        int i = (int)Math.round(d0 * 1000.0);
                        if (i <= 0) {
                            i = 80;
                        }

                        aint[j++] = Math.max(20, Math.min(5000, i));
                    } catch (Exception exception) {
                    }
                }

                if (j > 0) {
                    int k = aint[j - 1];

                    for (int l = j; l < aint.length; l++) {
                        aint[l] = k;
                    }
                }
            } catch (Exception exception1) {
            }

            return aint;
        }
    }

    private static String findBin(String name) {
        ensureBins();
        return !"ffmpeg".equals(name) && !"ffmpeg.exe".equals(name) ? cachedFfprobe : cachedFfmpeg;
    }

    private static void ensureBins() {
        if (!lookedUpBins) {
            lookedUpBins = true;
            cachedFfmpeg = locateBin("ffmpeg");
            cachedFfprobe = locateBin("ffprobe");
        }
    }

    private static String locateBin(String name) {
        List<String> list = new ArrayList<>();
        list.add(name);
        if (name.indexOf(46) < 0) {
            list.add(name + ".exe");
        }

        String s = System.getenv("LOCALAPPDATA");
        if (s != null) {
            list.add(Path.of(s, "Microsoft", "WinGet", "Links", name + ".exe").toString());
        }

        list.add("C:\\ffmpeg\\bin\\" + name + ".exe");

        for (String s1 : list) {
            try {
                ProcessBuilder processbuilder = new ProcessBuilder(s1, "-version");
                processbuilder.redirectErrorStream(true);
                Process process = processbuilder.start();
                drain(process);
                if (process.waitFor(4L, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    return s1;
                }
            } catch (Exception exception) {
            }
        }

        return null;
    }

    private static void drain(Process proc) {
        try (InputStream inputstream = proc.getInputStream()) {
            inputstream.readAllBytes();
        } catch (Exception exception) {
        }
    }

    public static GifIO.Frames read(Path gif) throws IOException {
        GifIO.Frames gifio$frames = ffmpegRead(gif);
        if (gifio$frames != null && !gifio$frames.images().isEmpty()) {
            return gifio$frames;
        } else if (isVideo(gif)) {
            throw new IOException("ffmpeg required for video banners");
        } else {
            GifIO.Frames gifio$frames1;
            try (InputStream inputstream = Files.newInputStream(gif)) {
                gifio$frames1 = read(inputstream);
            }

            return gifio$frames1;
        }
    }

    public static boolean isVideo(Path path) {
        if (path == null) {
            return false;
        } else {
            String s = path.getFileName().toString().toLowerCase(Locale.ROOT);
            return s.endsWith(".mp4") || s.endsWith(".webm") || s.endsWith(".mov") || s.endsWith(".mkv");
        }
    }

    public static GifIO.Frames read(InputStream in) throws IOException {
        Iterator<ImageReader> iterator = ImageIO.getImageReadersByFormatName("gif");
        if (!iterator.hasNext()) {
            throw new IOException("no gif reader");
        } else {
            ImageReader imagereader = pickGifReader(iterator);

            GifIO.Frames gifio$frames;
            try (ImageInputStream imageinputstream = ImageIO.createImageInputStream(in)) {
                if (imageinputstream == null) {
                    throw new IOException("gif stream");
                }

                imagereader.setInput(imageinputstream, false, false);
                int i = Math.max(1, imagereader.getNumImages(true));
                int j = i > 120 ? (int)Math.ceil(i / 120.0) : 1;
                int k = Math.max(1, safeDim(imagereader, true));
                int l = Math.max(1, safeDim(imagereader, false));
                int[] aint = logicalScreen(imagereader);
                if (aint[0] > 0 && aint[1] > 0) {
                    k = aint[0];
                    l = aint[1];
                }

                BufferedImage bufferedimage = new BufferedImage(k, l, 2);
                Graphics2D graphics2d = bufferedimage.createGraphics();
                graphics2d.setComposite(AlphaComposite.SrcOver);
                graphics2d.setBackground(new Color(0, 0, 0, 0));
                clear(graphics2d, 0, 0, k, l);
                List<BufferedImage> list = new ArrayList<>();
                List<Integer> list1 = new ArrayList<>();
                BufferedImage bufferedimage1 = null;
                int i1 = 0;
                int j1 = 0;
                int k1 = k;
                int l1 = l;
                String s = "none";

                for (int i2 = 0; i2 < i; i2++) {
                    BufferedImage bufferedimage2 = imagereader.read(i2);
                    int j2 = 0;
                    int k2 = 0;
                    int l2 = bufferedimage2.getWidth();
                    int i3 = bufferedimage2.getHeight();
                    int j3 = 80;
                    String s1 = "none";

                    try {
                        IIOMetadata iiometadata = imagereader.getImageMetadata(i2);
                        int[] aint1 = imageBox(iiometadata, bufferedimage2.getWidth(), bufferedimage2.getHeight());
                        j2 = aint1[0];
                        k2 = aint1[1];
                        l2 = aint1[2];
                        i3 = aint1[3];
                        j3 = delayMs(iiometadata);
                        s1 = disposalOf(iiometadata);
                    } catch (Exception exception) {
                    }

                    if (i2 > 0) {
                        applyDisposal(graphics2d, bufferedimage1, s, i1, j1, k1, l1);
                    }

                    if ("restoreToPrevious".equals(s1)) {
                        bufferedimage1 = copy(bufferedimage);
                    } else {
                        bufferedimage1 = null;
                    }

                    graphics2d.setComposite(AlphaComposite.SrcOver);
                    graphics2d.drawImage(bufferedimage2, j2, k2, null);
                    if (i2 % j == 0) {
                        list.add(copy(bufferedimage));
                        list1.add(j3);
                    }

                    i1 = j2;
                    j1 = k2;
                    k1 = l2;
                    l1 = i3;
                    s = s1;
                }

                graphics2d.dispose();
                if (list.isEmpty()) {
                    throw new IOException("empty gif");
                }

                int[] aint2 = new int[list.size()];

                for (int k3 = 0; k3 < aint2.length; k3++) {
                    aint2[k3] = list1.get(Math.min(k3, list1.size() - 1));
                }

                gifio$frames = new GifIO.Frames(list, aint2);
            } finally {
                imagereader.dispose();
            }

            return gifio$frames;
        }
    }

    public static int explode(Path gif, Path outDir) throws IOException {
        GifIO.Frames gifio$frames = read(gif);
        return writePngSequence(gifio$frames, outDir);
    }

    public static int explode(InputStream in, Path outDir) throws IOException {
        GifIO.Frames gifio$frames = read(in);
        return writePngSequence(gifio$frames, outDir);
    }

    public static int writePngSequence(GifIO.Frames frames, Path outDir) throws IOException {
        Files.createDirectories(outDir);

        try (Stream<Path> stream = Files.list(outDir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ioexception) {
                }
            });
        }

        int k = 80;
        int i = 0;

        for (int j = 0; j < frames.images().size(); j++) {
            Path path = outDir.resolve(String.format(Locale.ROOT, "frame_%03d.png", j));
            ImageIO.write(frames.images().get(j), "PNG", path.toFile());
            i += frames.delaysMs()[Math.min(j, frames.delaysMs().length - 1)];
        }

        k = Math.max(20, i / Math.max(1, frames.images().size()));
        StringBuilder stringbuilder = new StringBuilder();

        for (int l = 0; l < frames.delaysMs().length; l++) {
            if (l > 0) {
                stringbuilder.append(',');
            }

            stringbuilder.append(frames.delaysMs()[l]);
        }

        Files.writeString(outDir.resolve("delay.txt"), stringbuilder.toString());
        return k;
    }

    public static void writeLoopingGif(Path dest, List<BufferedImage> frames, int delayCs) throws IOException {
        Files.createDirectories(dest.getParent());
        Iterator<ImageWriter> iterator = ImageIO.getImageWritersByFormatName("gif");
        if (iterator.hasNext() && !frames.isEmpty()) {
            ImageWriter imagewriter = iterator.next();

            try (ImageOutputStream imageoutputstream = ImageIO.createImageOutputStream(dest.toFile())) {
                imagewriter.setOutput(imageoutputstream);
                imagewriter.prepareWriteSequence(null);
                ImageWriteParam imagewriteparam = imagewriter.getDefaultWriteParam();

                for (int i = 0; i < frames.size(); i++) {
                    BufferedImage bufferedimage = toRgb(frames.get(i));
                    IIOMetadata iiometadata = imagewriter.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(bufferedimage), imagewriteparam);
                    configureFrame(iiometadata, delayCs, i == 0);
                    imagewriter.writeToSequence(new IIOImage(bufferedimage, null, iiometadata), imagewriteparam);
                }

                imagewriter.endWriteSequence();
            } finally {
                imagewriter.dispose();
            }
        } else {
            throw new IOException("no gif writer");
        }
    }

    private static void configureFrame(IIOMetadata meta, int delayCs, boolean loop) {
        try {
            String s = meta.getNativeMetadataFormatName();
            IIOMetadataNode iiometadatanode = (IIOMetadataNode)meta.getAsTree(s);
            IIOMetadataNode iiometadatanode1 = child(iiometadatanode, "GraphicControlExtension");
            iiometadatanode1.setAttribute("disposalMethod", "none");
            iiometadatanode1.setAttribute("userInputFlag", "FALSE");
            iiometadatanode1.setAttribute("transparentColorFlag", "FALSE");
            iiometadatanode1.setAttribute("delayTime", Integer.toString(Math.max(2, delayCs)));
            iiometadatanode1.setAttribute("transparentColorIndex", "0");
            if (loop) {
                IIOMetadataNode iiometadatanode2 = child(iiometadatanode, "ApplicationExtensions");
                IIOMetadataNode iiometadatanode3 = new IIOMetadataNode("ApplicationExtension");
                iiometadatanode3.setAttribute("applicationID", "NETSCAPE");
                iiometadatanode3.setAttribute("authenticationCode", "2.0");
                iiometadatanode3.setUserObject(new byte[]{1, 0, 0});
                iiometadatanode2.appendChild(iiometadatanode3);
            }

            meta.setFromTree(s, iiometadatanode);
        } catch (Exception exception) {
        }
    }

    private static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (name.equals(root.item(i).getNodeName())) {
                return (IIOMetadataNode)root.item(i);
            }
        }

        IIOMetadataNode iiometadatanode = new IIOMetadataNode(name);
        root.appendChild(iiometadatanode);
        return iiometadatanode;
    }

    private static void applyDisposal(Graphics2D g, BufferedImage previous, String disposal, int x, int y, int w, int h) {
        if ("restoreToBackgroundColor".equals(disposal) || "restoreToBackground".equals(disposal)) {
            clear(g, x, y, w, h);
        } else if ("restoreToPrevious".equals(disposal) && previous != null) {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(previous, 0, 0, null);
            g.setComposite(AlphaComposite.SrcOver);
        }
    }

    private static void clear(Graphics2D g, int x, int y, int w, int h) {
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(x, y, w, h);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage bufferedimage = new BufferedImage(src.getWidth(), src.getHeight(), 2);
        Graphics2D graphics2d = bufferedimage.createGraphics();
        graphics2d.drawImage(src, 0, 0, null);
        graphics2d.dispose();
        return bufferedimage;
    }

    private static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == 1) {
            return src;
        } else {
            BufferedImage bufferedimage = new BufferedImage(src.getWidth(), src.getHeight(), 1);
            Graphics2D graphics2d = bufferedimage.createGraphics();
            graphics2d.setColor(new Color(6, 10, 14));
            graphics2d.fillRect(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight());
            graphics2d.drawImage(src, 0, 0, null);
            graphics2d.dispose();
            return bufferedimage;
        }
    }

    private static int safeDim(ImageReader reader, boolean width) {
        try {
            return width ? reader.getWidth(0) : reader.getHeight(0);
        } catch (Exception exception) {
            return 1;
        }
    }

    private static int[] logicalScreen(ImageReader reader) {
        try {
            IIOMetadata iiometadata = reader.getStreamMetadata();
            if (iiometadata == null) {
                return new int[]{0, 0};
            }

            Node node = iiometadata.getAsTree("javax_imageio_gif_stream_1.0");
            NodeList nodelist = node.getChildNodes();

            for (int i = 0; i < nodelist.getLength(); i++) {
                Node node1 = nodelist.item(i);
                if ("LogicalScreenDescriptor".equals(node1.getNodeName())) {
                    NamedNodeMap namednodemap = node1.getAttributes();
                    return new int[]{parse(namednodemap, "logicalScreenWidth", 0), parse(namednodemap, "logicalScreenHeight", 0)};
                }
            }
        } catch (Exception exception) {
        }

        return new int[]{0, 0};
    }

    private static int[] imageBox(IIOMetadata meta, int fw, int fh) {
        int i = 0;
        int j = 0;
        int k = fw;
        int l = fh;

        try {
            Node node = meta.getAsTree("javax_imageio_gif_image_1.0");
            NodeList nodelist = node.getChildNodes();

            for (int i1 = 0; i1 < nodelist.getLength(); i1++) {
                Node node1 = nodelist.item(i1);
                if ("ImageDescriptor".equals(node1.getNodeName())) {
                    NamedNodeMap namednodemap = node1.getAttributes();
                    i = parse(namednodemap, "imageLeftPosition", 0);
                    j = parse(namednodemap, "imageTopPosition", 0);
                    k = parse(namednodemap, "imageWidth", fw);
                    l = parse(namednodemap, "imageHeight", fh);
                }
            }
        } catch (Exception exception) {
        }

        return new int[]{i, j, k, l};
    }

    private static int delayMs(IIOMetadata meta) {
        try {
            Node node = meta.getAsTree("javax_imageio_gif_image_1.0");
            NodeList nodelist = node.getChildNodes();

            for (int i = 0; i < nodelist.getLength(); i++) {
                Node node1 = nodelist.item(i);
                if ("GraphicControlExtension".equals(node1.getNodeName())) {
                    int j = parse(node1.getAttributes(), "delayTime", 8);
                    if (j <= 0) {
                        j = 8;
                    }

                    return Math.max(20, Math.min(5000, j * 10));
                }
            }
        } catch (Exception exception) {
        }

        return 80;
    }

    private static String disposalOf(IIOMetadata meta) {
        try {
            Node node = meta.getAsTree("javax_imageio_gif_image_1.0");
            NodeList nodelist = node.getChildNodes();

            for (int i = 0; i < nodelist.getLength(); i++) {
                Node node1 = nodelist.item(i);
                if ("GraphicControlExtension".equals(node1.getNodeName())) {
                    Node node2 = node1.getAttributes().getNamedItem("disposalMethod");
                    return node2 == null ? "none" : node2.getNodeValue();
                }
            }
        } catch (Exception exception) {
        }

        return "none";
    }

    private static int parse(NamedNodeMap attrs, String key, int fallback) {
        if (attrs == null) {
            return fallback;
        } else {
            Node node = attrs.getNamedItem(key);
            if (node == null) {
                return fallback;
            } else {
                try {
                    return Integer.parseInt(node.getNodeValue());
                } catch (Exception exception) {
                    return fallback;
                }
            }
        }
    }

    public record Frames(List<BufferedImage> images, int[] delaysMs) {
    }
}
