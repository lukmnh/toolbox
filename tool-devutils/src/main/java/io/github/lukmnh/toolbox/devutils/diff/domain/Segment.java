package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.Objects;

/** Potongan teks satu baris; {@code changed} = kata yang berbeda dari pasangannya (di-highlight). */
public record Segment(String text, boolean changed) {

    public Segment {
        Objects.requireNonNull(text, "text");
    }
}
