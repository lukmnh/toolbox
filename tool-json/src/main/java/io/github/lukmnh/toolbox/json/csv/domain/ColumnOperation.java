package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.List;

/** Satu langkah pipeline yang sudah di-resolve ke indeks kolom. Dipakai bersama oleh pratinjau dan ekspor. */
sealed interface ColumnOperation permits SplitOperation, MergeOperation, ArrangeOperation {

    List<String> outputColumns();

    /** @param row lebar sama dengan kolom input langkah ini */
    List<String> apply(List<String> row, TimedMatcher matcher);
}
