package io.github.lukmnh.toolbox.json.csv.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Semua yang dibutuhkan untuk memproses satu file utuh ke {@code target}. */
public record ExportRequest(
        CsvSource source,
        InputFormat format,
        MismatchPolicy mismatchPolicy,
        List<PipelineStep> steps,
        OutputOptions output,
        Path target) {

    public ExportRequest {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(mismatchPolicy, "mismatchPolicy");
        steps = List.copyOf(steps);
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(target, "target");
    }
}
