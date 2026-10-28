package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.nio.file.Path;
import java.util.Objects;

/** Hasil decode Base64 → file biner di {@code output}, dengan tipe dari magic bytes. */
public record DecodedFile(Path output, long outputBytes, FileType type) {

    public DecodedFile {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(type, "type");
    }

    public String suggestedName() {
        return "decoded." + type.extension();
    }
}
