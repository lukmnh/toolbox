package io.github.lukmnh.toolbox.common.download;

import java.nio.file.Files;
import java.nio.file.Path;

/** Memberi nama yang belum dipakai: {@code hasil.pdf}, {@code hasil (1).pdf}, {@code hasil (2).pdf}, … */
public final class UniqueFileNamer {

    private UniqueFileNamer() {}

    public static Path uniqueFile(Path directory, String fileName) {
        int dot = fileName.lastIndexOf('.');
        boolean hasExtension = dot > 0;
        String stem = hasExtension ? fileName.substring(0, dot) : fileName;
        String extension = hasExtension ? fileName.substring(dot) : "";
        return firstFree(directory, stem, extension);
    }

    /** Untuk folder, titik di nama tidak dianggap ekstensi. */
    public static Path uniqueDirectory(Path directory, String directoryName) {
        return firstFree(directory, directoryName, "");
    }

    private static Path firstFree(Path directory, String stem, String extension) {
        Path candidate = directory.resolve(stem + extension);
        for (int counter = 1; Files.exists(candidate); counter++) {
            candidate = directory.resolve(stem + " (" + counter + ")" + extension);
        }
        return candidate;
    }
}
