package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.List;
import java.util.Objects;

/**
 * Data hasil baca file: nama kolom unik + baris dengan lebar sama. Dibuat lewat {@link DataTableBuilder}.
 *
 * @param sourceName nama file asal
 */
public record DataTable(String sourceName, List<String> columns, List<List<DataValue>> rows) {

    public DataTable {
        Objects.requireNonNull(sourceName, "sourceName");
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
        for (List<DataValue> row : rows) {
            if (row.size() != columns.size()) {
                throw new IllegalArgumentException("Row width does not match column count");
            }
        }
    }

    public int columnIndex(String column) {
        return columns.indexOf(column);
    }
}
