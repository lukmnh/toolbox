package io.github.lukmnh.toolbox.json.csv.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.json.csv.domain.ColumnNames;
import io.github.lukmnh.toolbox.json.csv.domain.CsvFiles;
import io.github.lukmnh.toolbox.json.csv.domain.CsvLimits;
import io.github.lukmnh.toolbox.json.csv.domain.CsvRecordReader;
import io.github.lukmnh.toolbox.json.csv.domain.CsvRow;
import io.github.lukmnh.toolbox.json.csv.domain.CsvSample;
import io.github.lukmnh.toolbox.json.csv.domain.CsvSource;
import io.github.lukmnh.toolbox.json.csv.domain.InputFormat;
import io.github.lukmnh.toolbox.json.csv.domain.MalformedCsvException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Baca header + {@link CsvLimits#PREVIEW_ROWS} record pertama dengan format yang dipilih. */
public final class LoadSampleUseCase {

    private final CsvFiles files;

    public LoadSampleUseCase(CsvFiles files) {
        this.files = files;
    }

    /** @throws MalformedCsvException bila file tidak berisi record sama sekali */
    public CsvSample execute(CsvSource source, InputFormat format, CancellationToken cancellation) {
        try (CsvRecordReader reader = files.openReader(source, format)) {
            CsvRow first = reader.next()
                    .orElseThrow(
                            () -> new MalformedCsvException(source.fileName(), MalformedCsvException.Kind.EMPTY, null));
            List<String> columns = ColumnNames.forFirstRecord(first.fields(), format.hasHeader());
            List<CsvRow> rows = new ArrayList<>(CsvLimits.PREVIEW_ROWS);
            if (!format.hasHeader()) {
                rows.add(first);
            }
            Optional<CsvRow> next = reader.next();
            while (next.isPresent() && rows.size() < CsvLimits.PREVIEW_ROWS) {
                cancellation.throwIfCancelled();
                rows.add(next.get());
                next = reader.next();
            }
            return new CsvSample(format, columns, rows, next.isPresent());
        }
    }
}
