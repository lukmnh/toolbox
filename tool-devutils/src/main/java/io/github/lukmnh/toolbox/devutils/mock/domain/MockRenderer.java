package io.github.lukmnh.toolbox.devutils.mock.domain;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Format baris ke JSON (array of object, indent 2), CSV (RFC 4180 + header), atau SQL (satu INSERT per baris).
 * {@code csvCell} disuntik dari composition root ({@code CsvFormulaGuard::neutralize}) karena domain hanya boleh JDK.
 *
 * <p>String SQL memakai escaping standar ({@code '} → {@code ''}); backslash tidak di-escape karena perlakuannya
 * berbeda antar database.
 */
public final class MockRenderer {

    /** Interval pemanggilan {@code checkpoint} (pembatalan + progress). */
    public static final int CHECKPOINT_ROWS = 1_000;

    private static final String CSV_LINE_END = "\r\n";

    private final UnaryOperator<String> csvCell;

    public MockRenderer(UnaryOperator<String> csvCell) {
        this.csvCell = csvCell;
    }

    @FunctionalInterface
    private interface RowWriter {
        void write(List<String> values, boolean first) throws IOException;
    }

    /**
     * @param limit jumlah baris maksimal yang ditulis (preview), atau {@code schema.rows()} untuk semua
     * @param checkpoint dipanggil tiap {@link #CHECKPOINT_ROWS} baris; boleh melempar untuk membatalkan
     */
    public void render(
            MockSchema schema,
            MockFormat format,
            Iterator<List<String>> rows,
            int limit,
            Appendable out,
            Runnable checkpoint)
            throws IOException {
        List<MockColumn> columns = schema.columns();
        switch (format) {
            case JSON -> {
                out.append('[');
                int written = writeRows(rows, limit, checkpoint, (values, first) -> {
                    out.append(first ? "\n  {" : ",\n  {");
                    for (int i = 0; i < columns.size(); i++) {
                        out.append(i == 0 ? "" : ", ")
                                .append(jsonString(columns.get(i).name()))
                                .append(": ")
                                .append(jsonValue(columns.get(i).type(), values.get(i)));
                    }
                    out.append('}');
                });
                out.append(written == 0 ? "]\n" : "\n]\n");
            }
            case CSV -> {
                out.append(columns.stream()
                                .map(column -> csvField(csvCell.apply(column.name())))
                                .collect(Collectors.joining(",")))
                        .append(CSV_LINE_END);
                writeRows(rows, limit, checkpoint, (values, first) -> {
                    for (int i = 0; i < columns.size(); i++) {
                        out.append(i == 0 ? "" : ",")
                                .append(csvValue(columns.get(i).type(), values.get(i)));
                    }
                    out.append(CSV_LINE_END);
                });
            }
            case SQL -> {
                String prefix = "INSERT INTO " + schema.table() + " ("
                        + columns.stream().map(MockColumn::name).collect(Collectors.joining(", "))
                        + ") VALUES (";
                writeRows(rows, limit, checkpoint, (values, first) -> {
                    out.append(prefix);
                    for (int i = 0; i < columns.size(); i++) {
                        out.append(i == 0 ? "" : ", ")
                                .append(sqlValue(columns.get(i).type(), values.get(i)));
                    }
                    out.append(");\n");
                });
            }
        }
    }

    private static int writeRows(Iterator<List<String>> rows, int limit, Runnable checkpoint, RowWriter writer)
            throws IOException {
        int written = 0;
        while (written < limit && rows.hasNext()) {
            writer.write(rows.next(), written == 0);
            written++;
            if (written % CHECKPOINT_ROWS == 0) {
                checkpoint.run();
            }
        }
        return written;
    }

    private static String jsonValue(MockType type, String value) {
        return type.kind() == MockType.Kind.TEXT ? jsonString(value) : value;
    }

    private String csvValue(MockType type, String value) {
        return type.kind() == MockType.Kind.TEXT ? csvField(csvCell.apply(value)) : value;
    }

    private static String sqlValue(MockType type, String value) {
        return type.kind() == MockType.Kind.TEXT ? sqlString(value) : value;
    }

    static String jsonString(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    /** RFC 4180: kutip bila ada koma, kutip, atau baris baru; kutip di dalam digandakan. */
    static String csvField(String value) {
        boolean quote = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        return quote ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    static String sqlString(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
