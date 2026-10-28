package io.github.lukmnh.toolbox.devutils.encoder.presentation;

/** Arah konversi; {@link #opposite()} dipakai tombol Tukar. */
enum Direction {
    ENCODE,
    DECODE;

    Direction opposite() {
        return this == ENCODE ? DECODE : ENCODE;
    }
}
