package io.github.lukmnh.toolbox.common.logging;

import io.github.lukmnh.toolbox.common.io.AppDirectories;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Menulis log WARNING ke atas (gagal, error, crash) ke file berotasi di folder {@code logs}. */
public final class FileLogging {

    private static final String APP_PATH_PROPERTY = "jpackage.app-path";
    private static final String LOG_DIRECTORY_NAME = "logs";
    private static final String FILE_PATTERN = "toolbox-%g.log";
    private static final int MAX_BYTES = 1024 * 1024;
    private static final int MAX_FILES = 5;
    private static final String FORMAT_PROPERTY = "java.util.logging.SimpleFormatter.format";
    private static final String FORMAT = "%1$tF %1$tT %4$s %3$s - %5$s%6$s%n";

    private static volatile Path activeDirectory;

    private FileLogging() {}

    /** Gagal pasang tidak membatalkan startup; folder berikutnya dicoba, alasan ditulis ke stderr. */
    public static Optional<Path> install() {
        System.setProperty(FORMAT_PROPERTY, System.getProperty(FORMAT_PROPERTY, FORMAT));
        for (Path directory : candidates(System.getProperty(APP_PATH_PROPERTY))) {
            try {
                Files.createDirectories(directory);
                FileHandler handler =
                        new FileHandler(directory.resolve(FILE_PATTERN).toString(), MAX_BYTES, MAX_FILES, true);
                handler.setEncoding(StandardCharsets.UTF_8.name());
                handler.setLevel(Level.WARNING);
                handler.setFormatter(new SimpleFormatter());
                Logger.getLogger("").addHandler(handler);
                activeDirectory = directory;
                return Optional.of(directory);
            } catch (IOException | SecurityException e) {
                System.err.println("Tidak bisa menulis log ke " + directory + ": " + e);
            }
        }
        return Optional.empty();
    }

    /** Folder log yang sedang dipakai; kosong bila {@link #install()} belum dipanggil atau gagal. */
    public static Optional<Path> directory() {
        return Optional.ofNullable(activeDirectory);
    }

    /** {@code logs} di samping aplikasi hasil jpackage (bisa read-only), fallback {@code ~/.toolbox/logs}. */
    static List<Path> candidates(String appPath) {
        List<Path> directories = new ArrayList<>();
        if (appPath != null && !appPath.isBlank()) {
            directories.add(appRoot(Path.of(appPath)).resolve(LOG_DIRECTORY_NAME));
        }
        directories.add(AppDirectories.dataDirectory().resolve(LOG_DIRECTORY_NAME));
        return directories;
    }

    /** Linux: {@code Toolbox/bin/Toolbox} → {@code Toolbox}. Windows: {@code Toolbox/Toolbox.exe} → {@code Toolbox}. */
    static Path appRoot(Path launcher) {
        Path parent = launcher.toAbsolutePath().getParent();
        return parent.getFileName() != null && parent.getFileName().toString().equals("bin")
                ? parent.getParent()
                : parent;
    }
}
