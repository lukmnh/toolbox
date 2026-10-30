package io.github.lukmnh.toolbox.pdf.merge.domain;

import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Gabung {@code sources} sesuai urutan list ke {@code output}.
 *
 * @param protection dengan {@link OutputProtection#KEEP}, hasil memakai proteksi sumber terenkripsi pertama
 */
public record MergeRequest(List<PdfSource> sources, Path output, OutputProtection protection) {

    public static final int MIN_SOURCES = 2;

    public MergeRequest {
        sources = List.copyOf(sources);
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(protection, "protection");
        if (sources.size() < MIN_SOURCES) {
            throw new IllegalArgumentException("At least " + MIN_SOURCES + " files are required to merge");
        }
    }
}
