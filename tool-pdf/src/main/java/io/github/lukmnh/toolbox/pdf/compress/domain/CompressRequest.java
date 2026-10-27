package io.github.lukmnh.toolbox.pdf.compress.domain;

import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.nio.file.Path;
import java.util.Objects;

public record CompressRequest(PdfSource source, CompressionPreset preset, Path output) {

    public CompressRequest {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(preset, "preset");
        Objects.requireNonNull(output, "output");
    }
}
