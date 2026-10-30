package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.util.Objects;

/** Hasil konversi teks beserta panjang input → output (karakter dan byte) untuk info di UI. */
public record TextConversion(String output, int inputChars, long inputBytes, int outputChars, long outputBytes) {

    public TextConversion {
        Objects.requireNonNull(output, "output");
        if (inputChars < 0 || inputBytes < 0 || outputChars < 0 || outputBytes < 0) {
            throw new IllegalArgumentException("Length cannot be negative");
        }
    }
}
