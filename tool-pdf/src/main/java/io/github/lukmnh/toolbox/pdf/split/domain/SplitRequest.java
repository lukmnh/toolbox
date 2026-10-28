package io.github.lukmnh.toolbox.pdf.split.domain;

import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @param outputDirectory folder (biasanya temp) tempat file hasil ditulis
 * @param baseName nama file asli tanpa ekstensi, untuk nama hasil {@code <baseName>_hal1-3.pdf}
 * @param protection perlakuan password sumber pada tiap file hasil
 */
public record SplitRequest(
        PdfSource source, SplitMode mode, Path outputDirectory, String baseName, OutputProtection protection) {

    public SplitRequest {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(protection, "protection");
        if (baseName == null || baseName.isBlank()) {
            throw new IllegalArgumentException("baseName wajib diisi");
        }
    }
}
