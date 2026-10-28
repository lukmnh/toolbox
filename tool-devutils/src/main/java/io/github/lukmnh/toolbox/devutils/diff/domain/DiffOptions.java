package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.Objects;

/**
 * Opsi perbandingan. Opsi hanya memengaruhi kunci pembanding baris ({@link LineKey}); teks asli tidak pernah
 * diubah sehingga file {@code .diff} tetap bisa di-apply ke file aslinya.
 */
public record DiffOptions(WhitespaceMode whitespace, boolean ignoreCase, boolean normalizeLineEndings) {

    public static final DiffOptions DEFAULT = new DiffOptions(WhitespaceMode.EXACT, false, true);

    public DiffOptions {
        Objects.requireNonNull(whitespace, "whitespace");
    }
}
