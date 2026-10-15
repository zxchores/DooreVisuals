package dev.doorelauncher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Keeps the DooreVisuals mod in the game's mods folder up to date: the jar that comes with the launcher is installed first,
 * then the latest release on GitHub replaces it when it is newer. Fabric API is inside the mod, so nothing else is needed.
 */
final class Mods {
    private static final String RELEASES = "https://api.github.com/repos/zxchores/DooreVisuals/releases/latest";
    private static final Pattern NAME = Pattern.compile("^doorevisuals-(\\d+(?:\\.\\d+)*)\\.jar$");

    private Mods() {
    }

    static void sync(Path appDir, Game.Progress p) throws IOException {
        Path mods = Game.HOME.resolve("game").resolve("mods");
        Files.createDirectories(mods);
        p.status("Проверка мода…");
        Path bundled = newest(appDir);
        Path installed = newest(mods);
        if (bundled != null && (installed == null || compare(version(bundled), version(installed)) > 0)) {
            install(bundled, mods);
            installed = bundled;
        }
        try {
            Map<String, Object> release = Json.obj(Json.parse(Http.text(RELEASES)));
            for (Object o : Json.list(release.get("assets"))) {
                Map<String, Object> asset = Json.obj(o);
                String name = Json.str(asset.get("name"));
                if (!NAME.matcher(name).matches()) {
                    continue;
                }
                String remote = version(Path.of(name));
                if (legacy(remote)) {
                    continue;
                }
                if (installed == null || compare(remote, version(installed)) > 0) {
                    p.status("Обновление мода " + remote + "…");
                    Path tmp = Game.HOME.resolve(name);
                    Http.download(Json.str(asset.get("browser_download_url")), tmp, null);
                    install(tmp, mods);
                    Files.deleteIfExists(tmp);
                }
                break;
            }
        } catch (Exception e) {
            // No internet, no release yet, or no access: the installed mod stays.
        }
        if (newest(mods) == null) {
            throw new IOException("Мод DooreVisuals не найден: положи doorevisuals-*.jar рядом с лаунчером.");
        }
    }

    /** The first releases of the repository (3.x) used an older numbering and need Fabric API installed separately; skipped. */
    private static boolean legacy(String version) {
        return Integer.parseInt(version.split("\\.")[0]) >= 3;
    }

    private static void install(Path jar, Path mods) throws IOException {
        try (Stream<Path> old = Files.list(mods)) {
            for (Path f : old.toList()) {
                if (NAME.matcher(f.getFileName().toString()).matches()) {
                    Files.delete(f);
                }
            }
        }
        Files.copy(jar, mods.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private static Path newest(Path dir) throws IOException {
        if (dir == null || !Files.isDirectory(dir)) {
            return null;
        }
        Path best = null;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.toList()) {
                if (NAME.matcher(f.getFileName().toString()).matches() && (best == null || compare(version(f), version(best)) > 0)) {
                    best = f;
                }
            }
        }
        return best;
    }

    private static String version(Path jar) {
        Matcher m = NAME.matcher(jar.getFileName().toString());
        return m.matches() ? m.group(1) : "0";
    }

    private static int compare(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? Integer.parseInt(x[i]) : 0;
            int q = i < y.length ? Integer.parseInt(y[i]) : 0;
            if (p != q) {
                return Integer.compare(p, q);
            }
        }
        return 0;
    }
}
