package io.github.lukmnh.toolbox.json.viewer.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonValue;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordColumn;
import io.github.lukmnh.toolbox.json.viewer.domain.SortKey;
import io.github.lukmnh.toolbox.json.viewer.domain.SortSpec;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

/**
 * Mengurutkan view berdasarkan satu kolom. Hanya nilai kolom itu yang diekstrak dari tiap record; sort stabil
 * sehingga nilai sama tetap berurutan sesuai view sebelumnya.
 */
public final class SortRecordsUseCase {

    private static final int CHECK_INTERVAL = 1024;

    public int[] execute(
            ViewerSession session,
            int[] rows,
            SortSpec spec,
            ProgressReporter progress,
            CancellationToken cancellation) {
        if (spec.column() instanceof RecordColumn.RecordNumber) {
            return byRecordNumber(rows, spec.ascending());
        }
        Entry[] entries = new Entry[rows.length];
        for (int i = 0; i < rows.length; i++) {
            if (i % CHECK_INTERVAL == 0) {
                cancellation.throwIfCancelled();
                progress.report(i, rows.length);
            }
            entries[i] = new Entry(rows[i], SortKey.of(extract(session, rows[i], spec.column())));
        }
        cancellation.throwIfCancelled();
        Comparator<SortKey> order = SortKey.order(spec.ascending());
        Arrays.sort(entries, (left, right) -> order.compare(left.key(), right.key()));
        progress.report(rows.length, rows.length);
        return Arrays.stream(entries).mapToInt(Entry::record).toArray();
    }

    private static Optional<JsonValue> extract(ViewerSession session, int record, RecordColumn column) {
        try {
            return column.extract(session.parse(session.raw(record)));
        } catch (MalformedJsonException e) {
            // Record rusak diperlakukan seperti nilai kosong: selalu di akhir
            return Optional.empty();
        }
    }

    private static int[] byRecordNumber(int[] rows, boolean ascending) {
        int[] sorted = rows.clone();
        Arrays.sort(sorted);
        if (!ascending) {
            for (int left = 0, right = sorted.length - 1; left < right; left++, right--) {
                int swap = sorted[left];
                sorted[left] = sorted[right];
                sorted[right] = swap;
            }
        }
        return sorted;
    }

    private record Entry(int record, SortKey key) {}
}
