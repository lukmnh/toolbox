package io.github.lukmnh.toolbox.pdf.compress.domain;

import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.nio.file.Path;
import java.util.Objects;

public record CompressRequest(PdfSource source, CompressionPreset preset, Path output, OutputProtection protection) {

    public CompressRequest {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(preset, "preset");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(protection, "protection");
    }
}
