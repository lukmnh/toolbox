package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.Objects;

/**
 * Satu potongan yang berbeda. Indeks 0-based, {@code end} eksklusif: {@code [oldStart, oldEnd)} di teks asli diganti
 * {@code [newStart, newEnd)} di teks baru.
 */
public record DiffChunk(ChunkType type, int oldStart, int oldEnd, int newStart, int newEnd) {

    public DiffChunk {
        Objects.requireNonNull(type, "type");
        if (oldStart < 0 || newStart < 0 || oldEnd < oldStart || newEnd < newStart) {
            throw new IllegalArgumentException("Invalid chunk range");
        }
    }

    public int oldLength() {
        return oldEnd - oldStart;
    }

    public int newLength() {
        return newEnd - newStart;
    }
}
