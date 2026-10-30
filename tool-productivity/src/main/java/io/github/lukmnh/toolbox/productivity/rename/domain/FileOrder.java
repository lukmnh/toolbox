package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.util.Comparator;
import java.util.List;

/** Urutan file di batch; menentukan nomor urut aturan penomoran. */
public enum FileOrder {
    NAME,
    MODIFIED,
    SIZE,
    MANUAL;

    private static final Comparator<FileEntry> BY_NAME =
            Comparator.comparing(FileEntry::name, String.CASE_INSENSITIVE_ORDER).thenComparing(FileEntry::name);

    public List<FileEntry> sort(List<FileEntry> files) {
        return files.stream().sorted(comparator()).toList();
    }

    private Comparator<FileEntry> comparator() {
        return switch (this) {
            case NAME -> BY_NAME;
            case MODIFIED -> Comparator.comparing(FileEntry::lastModified).thenComparing(BY_NAME);
            case SIZE -> Comparator.comparingLong(FileEntry::sizeBytes).thenComparing(BY_NAME);
            // Sort stream stabil, jadi comparator netral mempertahankan urutan hasil drag / tombol naik-turun
            case MANUAL -> (left, right) -> 0;
        };
    }
}
