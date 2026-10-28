package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.nio.file.Path;
import java.util.Objects;

/** Hasil encode file → teks Base64 di {@code output}. */
public record EncodedFile(Path output, long inputBytes, long outputBytes) {

    public EncodedFile {
        Objects.requireNonNull(output, "output");
    }
}
