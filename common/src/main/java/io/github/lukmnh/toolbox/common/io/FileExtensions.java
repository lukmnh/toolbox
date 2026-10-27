package io.github.lukmnh.toolbox.common.io;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/** Utilitas nama dan ekstensi file. Ekstensi selalu huruf kecil tanpa titik. */
public final class FileExtensions {

    private FileExtensions() {}

    /** Ekstensi huruf kecil tanpa titik, atau string kosong bila tidak ada. */
    public static String extensionOf(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean hasExtension(Path file, Set<String> allowedExtensions) {
        return allowedExtensions.contains(extensionOf(file));
    }

    /** Nama file tanpa ekstensi terakhir: {@code laporan.final.pdf} → {@code laporan.final}. */
    public static String baseName(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? name : name.substring(0, dot);
    }
}
