package io.github.lukmnh.toolbox.devutils.sql.domain;

import io.github.lukmnh.toolbox.devutils.sql.domain.InvalidDataException.Problem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Aturan bersama semua reader (CSV/TXT/XLSX/JSON): header wajib unik dan tidak kosong, baris kosong dilewati, baris
 * pendek diisi NULL, dan jumlah baris dibatasi.
 */
public final class DataTableBuilder {

    private final String sourceName;
    private final int maxRows;
    private final List<String> columns = new ArrayList<>();
    /** Key huruf kecil: PostgreSQL/Oracle melipat huruf identifier tanpa kutip, jadi "Name" dan "name" bentrok. */
    private final Map<String, Integer> indexByKey = new HashMap<>();

    private final List<List<DataValue>> rows = new ArrayList<>();
    private boolean hasHeader;

    public DataTableBuilder(String sourceName, int maxRows) {
        this.sourceName = sourceName;
        this.maxRows = maxRows;
    }

    public boolean hasHeader() {
        return hasHeader;
    }

    /**
     * Baris header (CSV/TXT/XLSX). Sel kosong di ujung kanan dibuang (sisa format Excel).
     *
     * @param line nomor baris di file (1-based), untuk pesan error
     */
    public void header(List<String> names, long line) {
        List<String> trimmed = new ArrayList<>(names.stream().map(String::strip).toList());
        while (!trimmed.isEmpty() && trimmed.getLast().isEmpty()) {
            trimmed.removeLast();
        }
        if (trimmed.isEmpty()) {
            throw invalid(Problem.NO_HEADER, line, "");
        }
        for (String name : trimmed) {
            addColumn(name, line);
        }
        hasHeader = true;
    }

    /** Baris data dengan urutan sesuai header. */
    public void row(List<DataValue> values, long line) {
        if (!hasHeader) {
            throw new IllegalStateException("Header must be set before rows");
        }
        if (values.stream().allMatch(DataValue::isNull)) {
            return;
        }
        for (int index = columns.size(); index < values.size(); index++) {
            if (!values.get(index).isNull()) {
                throw invalid(Problem.TOO_MANY_FIELDS, line, "");
            }
        }
        List<DataValue> row = new ArrayList<>(values.subList(0, Math.min(values.size(), columns.size())));
        addRow(row, line);
    }

    /** Satu object JSON; key baru menjadi kolom baru (baris sebelumnya bernilai NULL di kolom itu). */
    public void record(Map<String, DataValue> fields, long line) {
        hasHeader = true;
        List<DataValue> row = new ArrayList<>(Collections.nCopies(columns.size(), DataValue.NULL));
        for (Map.Entry<String, DataValue> field : fields.entrySet()) {
            Integer index = indexByKey.get(key(field.getKey()));
            if (index == null || !columns.get(index).equals(field.getKey())) {
                index = addColumn(field.getKey(), line);
                row.add(DataValue.NULL);
            }
            row.set(index, field.getValue());
        }
        addRow(row, line);
    }

    public DataTable build() {
        if (!hasHeader) {
            throw invalid(Problem.NO_HEADER, InvalidDataException.NO_LINE, "");
        }
        if (rows.isEmpty()) {
            throw invalid(Problem.NO_ROWS, InvalidDataException.NO_LINE, "");
        }
        List<List<DataValue>> padded = new ArrayList<>(rows.size());
        for (List<DataValue> row : rows) {
            while (row.size() < columns.size()) {
                row.add(DataValue.NULL);
            }
            padded.add(row);
        }
        return new DataTable(sourceName, columns, padded);
    }

    private int addColumn(String name, long line) {
        if (name.isBlank()) {
            throw invalid(Problem.BLANK_COLUMN, line, String.valueOf(columns.size() + 1));
        }
        if (indexByKey.containsKey(key(name))) {
            throw invalid(Problem.DUPLICATE_COLUMN, line, name);
        }
        indexByKey.put(key(name), columns.size());
        columns.add(name);
        return columns.size() - 1;
    }

    private void addRow(List<DataValue> row, long line) {
        if (rows.size() >= maxRows) {
            throw invalid(Problem.TOO_MANY_ROWS, line, String.valueOf(maxRows));
        }
        rows.add(row);
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private InvalidDataException invalid(Problem problem, long line, String detail) {
        return new InvalidDataException(sourceName, problem, line, detail);
    }
}
