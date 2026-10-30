package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.Objects;

/** Isi file teks yang sudah divalidasi (bukan biner, di bawah batas ukuran). */
public record LoadedText(String fileName, String text, long sizeBytes) {

    public LoadedText {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(text, "text");
    }
}
