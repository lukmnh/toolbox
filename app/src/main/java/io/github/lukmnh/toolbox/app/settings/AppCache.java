package io.github.lukmnh.toolbox.app.settings;

import io.github.lukmnh.toolbox.common.io.AppDirectories;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.logging.FileLogging;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Cache yang aman dihapus user: isi folder temp, log lama hasil rotasi, dan recent file JSON Viewer. Favorit, settings,
 * guide, dan tanda tangan tidak disentuh; recent tool direset terpisah lewat {@link ToolUsage#withoutRecent()}.
 */
public final class AppCache {

    private static final System.Logger LOG = System.getLogger(AppCache.class.getName());
    // Milik JSON Viewer; shell tidak boleh import module tool (ArchitectureTest), jadi dirujuk lewat nama file
    private static final String JSON_RECENT_FILE = "recent.json";
    // toolbox-0.log sedang dipegang FileHandler (terkunci di Windows); hanya hasil rotasi yang dihapus
    private static final Pattern ROTATED_LOG = Pattern.compile("toolbox-[1-9]\\d*\\.log");

    /**
     * @param freed byte yang berhasil dihapus
     * @param remaining byte yang gagal dihapus, biasanya karena file masih dipakai proses lain
     */
    public record Result(long freed, long remaining) {}

    private final Path dataDirectory;
    private final Path tempDirectory;
    private final Optional<Path> logDirectory;

    public AppCache(Path dataDirectory, Path tempDirectory, Optional<Path> logDirectory) {
        this.dataDirectory = dataDirectory;
        this.tempDirectory = tempDirectory;
        this.logDirectory = logDirectory;
    }

    public static AppCache forCurrentSession() {
        return new AppCache(AppDirectories.dataDirectory(), AppDirectories.tempDirectory(), FileLogging.directory());
    }

    /** Total byte yang akan dihapus {@link #clear()}. */
    public long size() {
        return sizeOf(tempDirectory)
                + fileTargets().stream().mapToLong(AppCache::sizeOf).sum();
    }

    public Result clear() {
        long before = size();
        TempFiles.purge(tempDirectory);
        fileTargets().forEach(AppCache::delete);
        long remaining = size();
        return new Result(Math.max(0, before - remaining), remaining);
    }

    private List<Path> fileTargets() {
        List<Path> targets = new ArrayList<>();
        logDirectory.ifPresent(directory -> targets.addAll(rotatedLogs(directory)));
        Path jsonRecent = dataDirectory.resolve(JSON_RECENT_FILE);
        if (Files.isRegularFile(jsonRecent)) {
            targets.add(jsonRecent);
        }
        return targets;
    }

    private static List<Path> rotatedLogs(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.filter(path ->
                            ROTATED_LOG.matcher(path.getFileName().toString()).matches())
                    .toList();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Gagal membaca folder log", e);
            return List.of();
        }
    }

    /** File yang hilang di tengah walk (dihapus proses lain) dihitung 0, bukan error. */
    private static long sizeOf(Path target) {
        if (!Files.exists(target)) {
            return 0;
        }
        try (Stream<Path> files = Files.walk(target)) {
            return files.filter(Files::isRegularFile)
                    .mapToLong(AppCache::fileSize)
                    .sum();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Gagal menghitung ukuran " + target.getFileName(), e);
            return 0;
        }
    }

    private static long fileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    private static void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Tidak dilempar: sisa byte dilaporkan lewat Result.remaining() ke user
            LOG.log(Level.WARNING, "Gagal menghapus " + file.getFileName(), e);
        }
    }
}
