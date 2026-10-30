package io.github.lukmnh.toolbox.common.io;

import io.github.lukmnh.toolbox.common.security.OwnerOnlyFiles;
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
 *
 * <p>Temp disimpan di folder owner-only {@link AppDirectories#tempDirectory()}, bukan {@code java.io.tmpdir}
 * yang bisa dibaca user lain. Sisa temp (misalnya setelah crash) dihapus lewat {@link #purge(Path)} saat startup
 * dan exit (Security Requirements #4).
 */
public final class TempFiles implements AutoCloseable {

    private static final System.Logger LOG = System.getLogger(TempFiles.class.getName());
    private static final String PREFIX = "toolbox-";

    private final Path root;
    private final List<Path> created = new ArrayList<>();

    public TempFiles() {
        this(AppDirectories.tempDirectory());
    }

    /** @param root folder induk temp; dibuat owner-only bila belum ada */
    public TempFiles(Path root) {
        this.root = root;
    }

    public synchronized Path createFile(String suffix) {
        try {
            Path file = Files.createTempFile(ensureDirectory(root), PREFIX, suffix);
            created.add(file);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create temp file", e);
        }
    }

    public synchronized Path createDirectory() {
        try {
            Path directory = Files.createTempDirectory(ensureDirectory(root), PREFIX);
            created.add(directory);
            return directory;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create temp folder", e);
        }
    }

    /** Folder temp aplikasi, untuk library yang butuh lokasi temp sendiri (misalnya scratch file PDFBox). */
    public static Path applicationDirectory() {
        try {
            return ensureDirectory(AppDirectories.tempDirectory());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not prepare temp folder", e);
        }
    }

    @Override
    public synchronized void close() {
        created.forEach(TempFiles::deleteRecursively);
        created.clear();
    }

    /** Hapus seluruh isi {@code root}. Dipanggil saat startup (sisa crash) dan dari shutdown hook. */
    public static void purge(Path root) {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> entries = Files.list(root)) {
            entries.forEach(TempFiles::deleteRecursively);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Gagal membersihkan folder temp", e);
        }
    }

    private static Path ensureDirectory(Path directory) throws IOException {
        return Files.isDirectory(directory) ? directory : OwnerOnlyFiles.createDirectories(directory);
    }

    private static void deleteRecursively(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> entries = Files.walk(path)) {
            entries.sorted(Comparator.reverseOrder()).forEach(TempFiles::deleteQuietly);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Gagal membersihkan temp " + path.getFileName(), e);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Temp yang gagal dihapus tidak mengganggu user; cukup dicatat (nama saja, tanpa path home)
            LOG.log(Level.WARNING, "Gagal menghapus temp " + path.getFileName(), e);
        }
    }
}
