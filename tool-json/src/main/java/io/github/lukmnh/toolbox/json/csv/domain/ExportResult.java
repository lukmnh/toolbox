package io.github.lukmnh.toolbox.json.csv.domain;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Ringkasan proses ekspor.
 *
 * @param rowsWritten baris data yang ditulis, tanpa header
 */
public record ExportResult(
        Path output,
        long rowsWritten,
        int columnCount,
        long outputBytes,
        MismatchReport mismatches,
        Duration duration) {

    public ExportResult {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(mismatches, "mismatches");
        Objects.requireNonNull(duration, "duration");
    }
}
