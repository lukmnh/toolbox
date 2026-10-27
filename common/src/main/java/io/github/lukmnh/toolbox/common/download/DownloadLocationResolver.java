package io.github.lukmnh.toolbox.common.download;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Menentukan folder download default sesuai OS.
 *
 * <ul>
 *   <li>Windows: {@code %USERPROFILE%\Downloads}
 *   <li>Linux: {@code XDG_DOWNLOAD_DIR} dari {@code user-dirs.dirs}, fallback {@code ~/Downloads}
 * </ul>
 */
public final class DownloadLocationResolver {

    private static final System.Logger LOG = System.getLogger(DownloadLocationResolver.class.getName());
    private static final String DOWNLOADS = "Downloads";
    private static final String XDG_DOWNLOAD_KEY = "XDG_DOWNLOAD_DIR";
    private static final String USER_DIRS_FILE = "user-dirs.dirs";

    private final boolean windows;
    private final Path home;
    private final Map<String, String> environment;

    public DownloadLocationResolver(boolean windows, Path home, Map<String, String> environment) {
        this.windows = windows;
        this.home = home;
        this.environment = Map.copyOf(environment);
    }

    public static DownloadLocationResolver forCurrentSystem() {
        boolean windows =
                System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
        return new DownloadLocationResolver(windows, Path.of(System.getProperty("user.home")), System.getenv());
    }

    public Path resolve() {
        if (windows) {
            return windowsDownloads();
        }
        return xdgDownloadDirectory().orElse(home.resolve(DOWNLOADS));
    }

    private Path windowsDownloads() {
        String userProfile = environment.get("USERPROFILE");
        Path base = userProfile == null || userProfile.isBlank() ? home : Path.of(userProfile);
        return base.resolve(DOWNLOADS);
    }

    private Optional<Path> xdgDownloadDirectory() {
        Path userDirs = configHome().resolve(USER_DIRS_FILE);
        if (!Files.isRegularFile(userDirs)) {
            return Optional.empty();
        }
        try {
            return parseDownloadDirectory(Files.readAllLines(userDirs));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Gagal membaca " + userDirs + ", memakai ~/Downloads", e);
            return Optional.empty();
        }
    }

    private Path configHome() {
        String xdgConfigHome = environment.get("XDG_CONFIG_HOME");
        if (xdgConfigHome == null || xdgConfigHome.isBlank()) {
            return home.resolve(".config");
        }
        return Path.of(xdgConfigHome);
    }

    /** Format baris: {@code XDG_DOWNLOAD_DIR="$HOME/Downloads"}. */
    Optional<Path> parseDownloadDirectory(List<String> lines) {
        return lines.stream()
                .map(String::strip)
                .filter(line -> line.startsWith(XDG_DOWNLOAD_KEY + "="))
                .map(line -> line.substring(XDG_DOWNLOAD_KEY.length() + 1))
                .map(DownloadLocationResolver::unquote)
                .filter(value -> !value.isBlank())
                .map(this::expandHome)
                .reduce((first, last) -> last);
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private Path expandHome(String value) {
        String expanded = value.replace("${HOME}", home.toString()).replace("$HOME", home.toString());
        Path path = Path.of(expanded);
        return path.isAbsolute() ? path : home.resolve(path);
    }
}
