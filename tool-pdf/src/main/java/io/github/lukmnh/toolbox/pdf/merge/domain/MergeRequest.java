package io.github.lukmnh.toolbox.pdf.merge.domain;

import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Gabung {@code sources} sesuai urutan list ke {@code output}. */
public record MergeRequest(List<PdfSource> sources, Path output) {

    public static final int MIN_SOURCES = 2;

    public MergeRequest {
        sources = List.copyOf(sources);
        Objects.requireNonNull(output, "output");
        if (sources.size() < MIN_SOURCES) {
            throw new IllegalArgumentException("Minimal " + MIN_SOURCES + " file untuk digabung");
        }
    }
}
