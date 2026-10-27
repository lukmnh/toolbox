package io.github.lukmnh.toolbox.json.viewer.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Kolom data mode tabel, diambil dari key top-level record contoh (aturan BR-JS-6). */
public record TableColumns(List<RecordColumn> data) {

    /** Jumlah record pertama yang dipakai untuk menentukan kolom. */
    public static final int SAMPLE_SIZE = 1_000;

    public static final TableColumns NONE = new TableColumns(List.of());

    public TableColumns {
        data = List.copyOf(data);
    }

    /** Key sesuai urutan kemunculan. Kolom {@code value} ditambahkan bila ada record yang bukan object. */
    public static TableColumns sample(List<JsonValue> records) {
        Set<String> keys = new LinkedHashSet<>();
        boolean hasNonObject = false;
        for (JsonValue record : records) {
            if (record instanceof JsonValue.JsonObject object) {
                keys.addAll(object.fields().keySet());
            } else {
                hasNonObject = true;
            }
        }
        List<RecordColumn> columns = new ArrayList<>();
        if (hasNonObject) {
            columns.add(new RecordColumn.WholeValue());
        }
        keys.forEach(key -> columns.add(new RecordColumn.Field(key)));
        return new TableColumns(columns);
    }
}
