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

    public static Path models() {
        Path path = root().resolve("models");

        try {
            Files.createDirectories(path);
            Path path1 = path.resolve("PUT_MODEL_HERE.txt");
            if (!Files.exists(path1)) {
                Files.writeString(
                    path1,
                    "Put a folder per model: model.json (Blockbench bedrock/geo), texture.png, optional animation.json\n"
                );
            }
        } catch (Exception exception) {
        }

        return path;
    }

    public static Path invPresets() {
        Path path = root().resolve("inv_presets.json");
        if (!Files.exists(path)) {
            try {
                Files.writeString(path, "{\n  \"pvp\": [\"меч\", \"сфера\", \"\", \"\", \"\", \"\", \"\", \"\", \"яблоко\"],\n  \"farm\": [\"кирка\", \"лопата\", \"\", \"\", \"\", \"\", \"\", \"\", \"еда\"]\n}\n");
            } catch (Exception exception) {
            }
        }

        return path;
    }

    public static void ensureAll() {
        root();
        configs();
        themes();
        cosmetics();
        models();
        invPresets();
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
