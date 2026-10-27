package io.github.lukmnh.toolbox.common.io;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Menulis file tanpa pernah meninggalkan file tujuan dalam kondisi setengah jadi. */
public final class AtomicFiles {

    private static final String TEMP_SUFFIX = ".tmp";

    private AtomicFiles() {}

    /** Salin {@code source} ke {@code target}. Sumber tetap ada, sehingga bisa disalin ulang. */
    public static void copy(Path source, Path target) throws IOException {
        Path temp = siblingTemp(target);
        try {
            Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
            move(temp, target);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    /** Tulis lewat {@code writer} ke file sementara di folder yang sama, lalu pindahkan ke {@code target}. */
    public static void write(Path target, FileWriter writer) throws IOException {
        Path temp = siblingTemp(target);
        try {
            writer.writeTo(temp);
            move(temp, target);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    /** Atomic move dengan fallback untuk file system yang tidak mendukungnya (misalnya beda drive). */
    public static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path siblingTemp(Path target) throws IOException {
        Path directory = target.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        // Temp di folder yang sama supaya move tetap atomic (satu file system)
        return Files.createTempFile(directory, "." + target.getFileName(), TEMP_SUFFIX);
    }

    @FunctionalInterface
    public interface FileWriter {
        void writeTo(Path file) throws IOException;
    }
}
