package io.github.lukmnh.toolbox.json.csv.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.json.csv.domain.ColumnNames;
import io.github.lukmnh.toolbox.json.csv.domain.ColumnPipeline;
import io.github.lukmnh.toolbox.json.csv.domain.CsvFiles;
import io.github.lukmnh.toolbox.json.csv.domain.CsvLimits;
import io.github.lukmnh.toolbox.json.csv.domain.CsvRecordReader;
import io.github.lukmnh.toolbox.json.csv.domain.CsvRecordWriter;
import io.github.lukmnh.toolbox.json.csv.domain.CsvRow;
import io.github.lukmnh.toolbox.json.csv.domain.ExportRequest;
import io.github.lukmnh.toolbox.json.csv.domain.ExportResult;
import io.github.lukmnh.toolbox.json.csv.domain.MalformedCsvException;
import io.github.lukmnh.toolbox.json.csv.domain.OutputFormat;
import io.github.lukmnh.toolbox.json.csv.domain.RowNormalizer;
import io.github.lukmnh.toolbox.json.csv.domain.TimedMatcher;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** Proses file utuh secara streaming: baca record → normalisasi lebar → pipeline → tulis ke file hasil. */
public final class ExportCsvUseCase {

    private final CsvFiles files;
    private final TimedMatcher matcher;

    public ExportCsvUseCase(CsvFiles files, TimedMatcher matcher) {
        this.files = files;
        this.matcher = matcher;
    }

    /**
     * Progress dilaporkan dalam byte input; pembatalan dicek tiap {@link CsvLimits#PROGRESS_INTERVAL_ROWS} baris.
     *
     * @throws io.github.lukmnh.toolbox.json.csv.domain.CsvToolException untuk file rusak, encoding salah, I/O, atau
     *     langkah tidak valid
     */
    public ExportResult execute(ExportRequest request, ProgressReporter progress, CancellationToken cancellation) {
        long started = System.nanoTime();
        OutputFormat format = request.output().resolve(request.format().delimiter());
        String displayName = request.source().resultFileName();
        try (CsvRecordReader reader = files.openReader(request.source(), request.format());
                CsvRecordWriter writer = files.openWriter(request.target(), displayName, format)) {
            CsvRow first = reader.next()
                    .orElseThrow(() -> new MalformedCsvException(
                            request.source().fileName(), MalformedCsvException.Kind.EMPTY, null));
            boolean hasHeader = request.format().hasHeader();
            List<String> columns = ColumnNames.forFirstRecord(first.fields(), hasHeader);
            ColumnPipeline pipeline = ColumnPipeline.compile(columns, request.steps());
            if (request.output().includeHeader()) {
                writer.write(pipeline.outputColumns());
            }
            Run run = new Run(pipeline, new RowNormalizer(columns.size(), request.mismatchPolicy()), writer);
            if (!hasHeader) {
                run.write(first);
            }
            run.drain(reader, request.source().sizeBytes(), progress, cancellation);
            return run.result(request, started);
        }
    }

    /** State satu proses ekspor. */
    private final class Run {

        private final ColumnPipeline pipeline;
        private final RowNormalizer normalizer;
        private final CsvRecordWriter writer;
        private long rowsWritten;

        private Run(ColumnPipeline pipeline, RowNormalizer normalizer, CsvRecordWriter writer) {
            this.pipeline = pipeline;
            this.normalizer = normalizer;
            this.writer = writer;
        }

        void drain(CsvRecordReader reader, long totalBytes, ProgressReporter progress, CancellationToken cancellation) {
            long rowsRead = 0;
            for (Optional<CsvRow> row = reader.next(); row.isPresent(); row = reader.next()) {
                write(row.get());
                if (++rowsRead % CsvLimits.PROGRESS_INTERVAL_ROWS == 0) {
                    cancellation.throwIfCancelled();
                    progress.report(Math.min(reader.bytesRead(), totalBytes), totalBytes);
                }
            }
            progress.report(totalBytes, totalBytes);
        }

        void write(CsvRow row) {
            Optional<List<String>> normalized = normalizer.normalize(row);
            if (normalized.isPresent()) {
                writer.write(pipeline.apply(normalized.get(), matcher));
                rowsWritten++;
            }
        }

        ExportResult result(ExportRequest request, long startedNanos) {
            // Writer ditutup dulu supaya ukuran file hasil sudah final
            writer.close();
            return new ExportResult(
                    request.target(),
                    rowsWritten,
                    pipeline.outputColumns().size(),
                    files.size(request.target()),
                    normalizer.report(),
                    Duration.ofNanos(System.nanoTime() - startedNanos));
        }
    }
}
