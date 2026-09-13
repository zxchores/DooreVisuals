package dev.doorevisuals.data;

import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ClientPaths {
    private ClientPaths() {
    }

    public static Path appRoot() {
        String s = System.getenv("APPDATA");
        if (s != null && !s.isBlank()) {
            Path path1 = Path.of(s, ".doorevisuals");

            try {
                Files.createDirectories(path1);
            } catch (Exception exception) {
            }

            return path1;
        } else {
            Path path = root().getParent();
            return path != null && path.getParent() != null ? path.getParent() : root();
        }
    }

    public static Path accountFile() {
        return appRoot().resolve("account.json");
    }

    public static Path privilegesFile() {
        return appRoot().resolve("privileges.json");
    }

    public static Path root() {
        Path path = FabricLoader.getInstance().getGameDir().resolve("doorevisuals");

        try {
            Files.createDirectories(path);
        } catch (Exception exception) {
        }

        return path;
    }

    public static Path configs() {
        Path path = root().resolve("configs");

        try {
            Files.createDirectories(path);
        } catch (Exception exception) {
        }

        return path;
    }

    public static Path themes() {
        Path path = root().resolve("themes");

        try {
            Files.createDirectories(path);
        } catch (Exception exception) {
        }

        return path;
    }

    public static Path banners() {
        Path path = root().resolve("banners");

        try {
            Files.createDirectories(path);
        } catch (Exception exception) {
        }

        return path;
    }

    public static void ensureAll() {
        root();
        configs();
        themes();
        cosmetics();
        Path path = banners();
        Path path1 = path.resolve("PUT_PNG_GIF_OR_MP4_HERE.txt");
        if (!Files.exists(path1)) {
            try {
                Files.writeString(path1, "Put PNG, GIF or MP4 here.\nThe file shows up in ClickGUI \u2192 Settings \u2192 Profile banner.\n");
            } catch (Exception exception) {
            }
        }
    }

    public static Path cosmetics() {
        Path path = appRoot().resolve("cosmetics");

        try {
            Files.createDirectories(path);
        } catch (Exception exception) {
        }

        return path;
    }

    public static Path gps() {
        return root().resolve("gps.json");
    }

    public static Path state() {
        return root().resolve("state.json");
    }
}
