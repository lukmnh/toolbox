package io.github.lukmnh.toolbox.productivity.text.presentation;

import io.github.lukmnh.toolbox.common.ui.Tone;
import java.util.Objects;

/** Teks + warna badge indikator batas karakter. Teks kosong = indikator disembunyikan. */
record LimitIndicator(String text, Tone tone) {

    static final LimitIndicator HIDDEN = new LimitIndicator("", Tone.NEUTRAL);

    LimitIndicator {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(tone, "tone");
    }
}
