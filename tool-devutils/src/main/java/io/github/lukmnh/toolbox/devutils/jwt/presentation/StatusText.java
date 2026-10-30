package io.github.lukmnh.toolbox.devutils.jwt.presentation;

import io.github.lukmnh.toolbox.common.ui.Tone;
import java.util.Objects;

/** Teks + nada warna untuk badge dan pesan inline. */
record StatusText(String text, Tone tone) {

    static final StatusText EMPTY = new StatusText("", Tone.NEUTRAL);

    StatusText {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(tone, "tone");
    }
}
