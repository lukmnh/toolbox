package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.List;
import java.util.Objects;

/**
 * Satu baris side-by-side.
 *
 * @param oldNumber nomor baris di teks asli (mulai 1), atau {@link #NO_LINE} bila sisi ini kosong
 * @param newNumber nomor baris di teks baru (mulai 1), atau {@link #NO_LINE}
 */
public record DiffRow(
        RowType type, int oldNumber, List<Segment> oldSegments, int newNumber, List<Segment> newSegments) {

    public static final int NO_LINE = 0;

    public DiffRow {
        Objects.requireNonNull(type, "type");
        oldSegments = List.copyOf(oldSegments);
        newSegments = List.copyOf(newSegments);
    }

    public boolean hasOld() {
        return oldNumber != NO_LINE;
    }

    public boolean hasNew() {
        return newNumber != NO_LINE;
    }
}
