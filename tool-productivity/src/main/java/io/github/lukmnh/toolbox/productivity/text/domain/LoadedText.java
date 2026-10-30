package io.github.lukmnh.toolbox.productivity.text.domain;

import java.util.Objects;

/** Isi file teks yang sudah di-decode (UTF-8, baris baru dinormalisasi ke {@code \n}). */
public record LoadedText(String fileName, String text) {

    public LoadedText {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(text, "text");
    }
}
