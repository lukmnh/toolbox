package io.github.lukmnh.toolbox.common.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Pencatat temp file milik satu tool. Semua yang dibuat lewat instance ini dihapus di {@link #close()},
 * yang dipanggil dari {@code Tool.dispose()}.
 */
public final class TempFiles implements AutoCloseable {

    private static final System.Logger LOG = System.getLogger(TempFiles.class.getName());
    private static final String PREFIX = "toolbox-";

    private final List<Path> created = new ArrayList<>();

    public synchronized Path createFile(String suffix) {
        try {
            Path file = Files.createTempFile(PREFIX, suffix);
            created.add(file);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Gagal membuat temp file", e);
        }
    }

    public synchronized Path createDirectory() {
        try {
            Path directory = Files.createTempDirectory(PREFIX);
            created.add(directory);
            return directory;
        } catch (IOException e) {
            throw new UncheckedIOException("Gagal membuat temp folder", e);
        }
    }

    @Override
    public synchronized void close() {
        created.forEach(TempFiles::deleteRecursively);
        created.clear();
    }

    private static void deleteRecursively(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> entries = Files.walk(path)) {
            entries.sorted(Comparator.reverseOrder()).forEach(TempFiles::deleteQuietly);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Gagal membersihkan temp " + path, e);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Temp yang gagal dihapus tidak mengganggu user; cukup dicatat
            LOG.log(Level.WARNING, "Gagal menghapus temp " + path, e);
        }
    }
}
