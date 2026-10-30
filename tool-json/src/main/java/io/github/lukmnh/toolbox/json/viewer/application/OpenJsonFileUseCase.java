package io.github.lukmnh.toolbox.json.viewer.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonFiles;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonValue;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordFile;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordIndex;
import io.github.lukmnh.toolbox.json.viewer.domain.TableColumns;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Index file JSON lalu buka untuk dibaca. Kolom tabel diambil dari 1.000 record pertama. */
public final class OpenJsonFileUseCase {

    private final JsonFiles files;

    public OpenJsonFileUseCase(JsonFiles files) {
        this.files = files;
    }

    public ViewerSession execute(Path file, ProgressReporter progress, CancellationToken cancellation) {
        RecordIndex index = files.index(file, (done, total) -> {
            cancellation.throwIfCancelled();
            progress.report(done, total);
        });
        RecordFile records = files.open(file);
        try {
            TableColumns columns =
                    index.format().isTable() ? sampleColumns(index, records, cancellation) : TableColumns.NONE;
            return new ViewerSession(index, records, columns);
        } catch (RuntimeException e) {
            records.close();
            throw e;
        }
    }

    private static TableColumns sampleColumns(RecordIndex index, RecordFile records, CancellationToken cancellation) {
        int sampleSize = Math.min(TableColumns.SAMPLE_SIZE, index.recordCount());
        List<JsonValue> sample = new ArrayList<>(sampleSize);
        for (int record = 0; record < sampleSize; record++) {
            cancellation.throwIfCancelled();
            try {
                sample.add(records.parse(records.read(index.start(record), (int) index.length(record))));
            } catch (MalformedJsonException e) {
                // Baris JSONL rusak tidak ikut menentukan kolom; tetap tampil di tabel sebagai sel tidak valid
            }
        }
        return TableColumns.sample(sample);
    }
}
