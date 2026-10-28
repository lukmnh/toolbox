package io.github.lukmnh.toolbox.common.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolusi path yang berasal dari isi file tidak dipercaya (misalnya baris file {@code SHA256SUMS}) supaya tidak
 * bisa keluar dari folder dasar (Security Requirements #6).
 */
public final class SafePaths {

    private SafePaths() {}

    /**
     * @param baseDirectory folder yang menjadi batas
     * @param relativeName path relatif dari input tidak dipercaya
     * @return path di dalam {@code baseDirectory}, atau kosong bila absolut, keluar folder (termasuk lewat symlink),
     *     atau tidak valid untuk OS ini
     */
    public static Optional<Path> resolveInside(Path baseDirectory, String relativeName) {
        if (relativeName.isBlank() || looksAbsolute(relativeName)) {
            return Optional.empty();
        }
        try {
            Path base = baseDirectory.toAbsolutePath().normalize();
            Path relative = Path.of(relativeName);
            if (relative.isAbsolute()) {
                return Optional.empty();
            }
            Path candidate = base.resolve(relative).normalize();
            if (!candidate.startsWith(base) || candidate.equals(base)) {
                return Optional.empty();
            }
            return staysInsideAfterLinks(base, candidate) ? Optional.of(candidate) : Optional.empty();
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    /** {@code /etc/passwd} tidak absolut di Windows, dan {@code C:\x} tidak absolut di Linux; tolak keduanya. */
    private static boolean looksAbsolute(String name) {
        return name.startsWith("/") || name.startsWith("\\") || (name.length() > 1 && name.charAt(1) == ':');
    }

    /** File yang belum ada tidak bisa di-{@code toRealPath}; cukup cek normalisasi. */
    private static boolean staysInsideAfterLinks(Path base, Path candidate) {
        if (!Files.exists(candidate)) {
            return true;
        }
        try {
            return candidate.toRealPath().startsWith(base.toRealPath());
        } catch (IOException e) {
            return false;
        }
    }
}
