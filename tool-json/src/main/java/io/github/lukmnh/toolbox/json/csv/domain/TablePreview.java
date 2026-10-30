package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.List;

/** Tabel kecil untuk ditampilkan; setiap baris selebar {@code columns}. */
public record TablePreview(List<String> columns, List<List<String>> rows) {

    public TablePreview {
        columns = List.copyOf(columns);
        rows = rows.stream().map(List::copyOf).toList();
    }
}
