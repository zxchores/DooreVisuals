package dev.doorevisuals.data;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Folders {
    private Folders() {
    }

    public static Path ensure(Path dir) {
        if (dir == null) {
            return null;
        } else {
            try {
                Files.createDirectories(dir);
            } catch (Exception exception) {
            }

            return dir;
        }
    }

    public static boolean open(Path dir) {
        Path path = ensure(dir);
        if (path == null) {
            return false;
        } else {
            Thread thread = new Thread(() -> {
                try {
                    openNow(path.toAbsolutePath());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }, "doore-open-folder");
            thread.setDaemon(true);
            thread.start();
            return true;
        }
    }

    public static String pretty(Path dir) {
        return dir == null ? "" : dir.toAbsolutePath().normalize().toString();
    }

    public static void pickImage(Consumer<Path> onPicked, Runnable onUnavailable) {
        Thread thread = new Thread(() -> {
            try {
                Path path = pickImageNow();
                if (path != null && onPicked != null) {
                    onPicked.accept(path);
                    return;
                }

                if (path == null) {
                    return;
                }
            } catch (Exception exception) {
                if (onUnavailable != null) {
                    onUnavailable.run();
                }
            }
        }, "doore-pick-file");
        thread.setDaemon(true);
        thread.start();
    }

    private static Path pickImageNow() throws Exception {
        String s = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        ProcessBuilder processbuilder;
        if (s.contains("win")) {
            processbuilder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-STA",
                "-Command",
                "Add-Type -AssemblyName System.Windows.Forms; $d = New-Object System.Windows.Forms.OpenFileDialog; $d.Title = 'DooreVisuals'; $d.Filter = 'Media|*.png;*.gif;*.jpg;*.jpeg;*.webp;*.mp4;*.webm;*.mov|GIF|*.gif|Video|*.mp4;*.webm;*.mov'; $d.Multiselect = $false; if ($d.ShowDialog() -eq 'OK') { [Console]::OutputEncoding = [Text.UTF8Encoding]::new(); [Console]::Write($d.FileName) }"
            );
        } else if (!s.contains("mac") && !s.contains("darwin")) {
            processbuilder = new ProcessBuilder(
                "zenity", "--file-selection", "--title=DooreVisuals", "--file-filter=Media | *.png *.gif *.jpg *.jpeg *.webp *.mp4 *.webm *.mov"
            );
        } else {
            processbuilder = new ProcessBuilder(
                "osascript",
                "-e",
                "POSIX path of (choose file of type {\"public.png\",\"com.compuserve.gif\",\"public.jpeg\",\"public.webp\",\"public.mpeg-4\",\"org.webmproject.webm\",\"com.apple.quicktime-movie\"} with prompt \"DooreVisuals\")"
            );
        }

        processbuilder.redirectErrorStream(true);
        Process process = processbuilder.start();
        String s1 = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (!process.waitFor(3L, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new IllegalStateException("file dialog timeout");
        } else if (process.exitValue() != 0 && s1.isEmpty()) {
            return null;
        } else if (!s1.isEmpty() && !s1.startsWith("error") && !s1.contains("User canceled")) {
            Path path = Path.of(s1.replace("\r", "").split("\n")[0].trim());
            return Files.isRegularFile(path) ? path : null;
        } else {
            return null;
        }
    }

    private static void openNow(Path dir) throws Exception {
        String s = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String s1 = dir.toString();
        if (s.contains("win")) {
            new ProcessBuilder("explorer.exe", s1).start();
        } else if (!s.contains("mac") && !s.contains("darwin")) {
            new ProcessBuilder("xdg-open", s1).start();
        } else {
            new ProcessBuilder("open", s1).start();
        }
    }
}
