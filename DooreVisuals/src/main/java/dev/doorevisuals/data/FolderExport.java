package dev.doorevisuals.data;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class FolderExport {
    private FolderExport() {
    }

    public static Path exportThemes() throws IOException {
        return zipDir(ClientPaths.themes(), "themes");
    }

    public static Path exportConfigs() throws IOException {
        return zipDir(ClientPaths.configs(), "configs");
    }

    public static boolean openParent(Path file) {
        if (file == null) {
            return false;
        } else {
            Path path = file.getParent();
            return path != null && Folders.open(path);
        }
    }

    private static Path zipDir(Path dir, String label) throws IOException {
        Files.createDirectories(dir);
        Files.createDirectories(ClientPaths.root());
        String s = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path path = ClientPaths.root().resolve(label + "-" + s + ".zip");

        try (
            OutputStream outputstream = Files.newOutputStream(path);
            ZipOutputStream zipoutputstream = new ZipOutputStream(outputstream);
            DirectoryStream<Path> directorystream = Files.newDirectoryStream(dir, "*.json");
        ) {
            boolean flag = false;

            for (Path path1 : directorystream) {
                flag = true;
                zipoutputstream.putNextEntry(new ZipEntry(path1.getFileName().toString()));
                Files.copy(path1, zipoutputstream);
                zipoutputstream.closeEntry();
            }

            if (!flag) {
                zipoutputstream.putNextEntry(new ZipEntry(".keep"));
                zipoutputstream.write(new byte[0]);
                zipoutputstream.closeEntry();
            }
        }

        return path;
    }
}
