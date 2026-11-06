package io.github.lukmnh.toolbox.devutils.sql.domain;

import io.github.lukmnh.toolbox.devutils.shared.domain.JsonText;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataValue.Kind;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generate INSERT/UPDATE/DELETE untuk PostgreSQL/Oracle, atau insertMany/updateOne/deleteOne untuk MongoDB.
 *
 * <p>Nilai {@link Kind#RAW} (CSV/TXT) diberi tipe per kolom: angka hanya bila semua nilai di kolom itu angka, supaya
 * kolom kode seperti {@code 00123} atau NIK 16 digit tetap teks. UPDATE/DELETE melewati baris dengan key kosong, karena
 * {@code WHERE key IS NULL} bisa mengenai banyak baris.
 */
public final class SyntaxGenerator {

    /** Interval pemanggilan {@code checkpoint} (pembatalan). */
    public static final int CHECKPOINT_ROWS = 1_000;

    /** Maks 15 digit di depan koma (presisi double/JavaScript) dan tanpa nol di depan. */
    private static final Pattern NUMBER = Pattern.compile("-?(0|[1-9]\\d{0,14})(\\.\\d+)?");

    private static final Pattern SIMPLE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** Reserved word PostgreSQL/Oracle yang sering jadi nama kolom; nama ini wajib di-quote. */
    private static final Set<String> RESERVED = Set.of(
            "ACCESS",
            "ALL",
            "AND",
            "ANY",
            "AS",
            "ASC",
            "BETWEEN",
            "BY",
            "CASE",
            "CHECK",
            "COLUMN",
            "COMMENT",
            "CONSTRAINT",
            "CREATE",
            "CURRENT",
            "DATE",
            "DEFAULT",
            "DELETE",
            "DESC",
            "DISTINCT",
            "DROP",
            "ELSE",
            "END",
            "EXISTS",
            "FILE",
            "FOR",
            "FROM",
            "GRANT",
            "GROUP",
            "HAVING",
            "IN",
            "INDEX",
            "INSERT",
            "INTO",
            "IS",
            "LEVEL",
            "LIKE",
            "LIMIT",
            "LOCK",
            "MODE",
            "NOT",
            "NULL",
            "NUMBER",
            "OF",
            "OFFSET",
            "ON",
            "OR",
            "ORDER",
            "PRIMARY",
            "REFERENCES",
            "RESOURCE",
            "ROW",
            "ROWNUM",
            "ROWS",
            "SELECT",
            "SESSION",
            "SET",
            "SIZE",
            "START",
            "TABLE",
            "THEN",
            "TO",
            "UID",
            "UNION",
            "UNIQUE",
            "UPDATE",
            "USER",
            "VALUES",
            "VIEW",
            "WHEN",
            "WHERE",
            "WITH");

    private SyntaxGenerator() {}

    /**
     * @param checkpoint dipanggil tiap {@link #CHECKPOINT_ROWS} baris; boleh melempar untuk membatalkan
     * @throws IllegalArgumentException bila key column tidak ada, atau UPDATE tanpa kolom selain key
     */
    public static GeneratedSyntax generate(DataTable table, GenerateOptions options, Runnable checkpoint) {
        int keyIndex = keyIndex(table, options);
        if (options.operation() == DmlOperation.UPDATE && table.columns().size() < 2) {
            throw new IllegalArgumentException("UPDATE needs at least one column besides the key");
        }
        Kind[] rawKinds = rawKinds(table);
        StringBuilder out = new StringBuilder();
        List<List<DataValue>> rows = new ArrayList<>();
        int skipped = 0;
        for (List<DataValue> row : table.rows()) {
            List<DataValue> resolved = resolve(row, rawKinds, options.allText());
            if (keyIndex >= 0 && resolved.get(keyIndex).isNull()) {
                skipped++;
            } else {
                rows.add(resolved);
            }
        }
        Statements statements = options.target() == SyntaxTarget.MONGODB
                ? new MongoStatements(table.columns(), options, keyIndex)
                : new SqlStatements(table.columns(), options, keyIndex);
        statements.write(rows, out, checkpoint);
        return new GeneratedSyntax(out.toString(), rows.size(), skipped);
    }

    private static int keyIndex(DataTable table, GenerateOptions options) {
        if (!options.operation().needsKey()) {
            return -1;
        }
        int index = table.columnIndex(options.keyColumn());
        if (index < 0) {
            throw new IllegalArgumentException("Unknown key column");
        }
        return index;
    }

    /** Tipe untuk nilai RAW per kolom: NUMBER/BOOLEAN bila seluruh nilai RAW di kolom cocok, selain itu TEXT. */
    private static Kind[] rawKinds(DataTable table) {
        Kind[] kinds = new Kind[table.columns().size()];
        for (int column = 0; column < kinds.length; column++) {
            boolean allNumbers = true;
            boolean allBooleans = true;
            for (List<DataValue> row : table.rows()) {
                DataValue value = row.get(column);
                if (value.kind() == Kind.RAW) {
                    String text = value.text().strip();
                    allNumbers &= NUMBER.matcher(text).matches();
                    allBooleans &= isBoolean(text);
                }
            }
            kinds[column] = allNumbers ? Kind.NUMBER : allBooleans ? Kind.BOOLEAN : Kind.TEXT;
        }
        return kinds;
    }

    private static List<DataValue> resolve(List<DataValue> row, Kind[] rawKinds, boolean allText) {
        List<DataValue> resolved = new ArrayList<>(row.size());
        for (int column = 0; column < row.size(); column++) {
            DataValue value = row.get(column);
            Kind kind = value.kind() == Kind.RAW ? rawKinds[column] : value.kind();
            if (allText && (kind == Kind.NUMBER || kind == Kind.BOOLEAN)) {
                kind = Kind.TEXT;
            }
            String text = kind == Kind.TEXT ? value.text() : value.text().strip();
            if (kind == Kind.BOOLEAN) {
                text = text.toLowerCase(Locale.ROOT);
            }
            resolved.add(kind == Kind.NULL ? DataValue.NULL : new DataValue(kind, text));
        }
        return resolved;
    }

    private static boolean isBoolean(String text) {
        return text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false");
    }

    /** Quote identifier hanya bila perlu, supaya identifier tanpa kutip tetap mengikuti case-folding database. */
    static String sqlIdentifier(String name) {
        boolean plain = SIMPLE_IDENTIFIER.matcher(name).matches() && !RESERVED.contains(name.toUpperCase(Locale.ROOT));
        return plain ? name : '"' + name.replace("\"", "\"\"") + '"';
    }

    /** Escaping standar SQL ({@code '} → {@code ''}); berlaku di PostgreSQL (standard_conforming_strings) dan Oracle. */
    static String sqlString(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private abstract static class Statements {

        final GenerateOptions options;
        final int keyIndex;

        Statements(GenerateOptions options, int keyIndex) {
            this.options = options;
            this.keyIndex = keyIndex;
        }

        void write(List<List<DataValue>> rows, StringBuilder out, Runnable checkpoint) {
            for (int index = 0; index < rows.size(); index++) {
                writeRow(rows.get(index), index, rows.size(), out);
                if ((index + 1) % CHECKPOINT_ROWS == 0) {
                    checkpoint.run();
                }
            }
        }

        abstract void writeRow(List<DataValue> row, int index, int total, StringBuilder out);
    }

    private static final class SqlStatements extends Statements {

        private final String table;
        private final List<String> quotedColumns;

        SqlStatements(List<String> columns, GenerateOptions options, int keyIndex) {
            super(options, keyIndex);
            this.table = List.of(options.table().split("\\.", -1)).stream()
                    .map(part -> sqlIdentifier(part.strip()))
                    .collect(Collectors.joining("."));
            this.quotedColumns =
                    columns.stream().map(SyntaxGenerator::sqlIdentifier).toList();
        }

        @Override
        void writeRow(List<DataValue> row, int index, int total, StringBuilder out) {
            switch (options.operation()) {
                case INSERT -> {
                    out.append("INSERT INTO ")
                            .append(table)
                            .append(" (")
                            .append(String.join(", ", quotedColumns))
                            .append(") VALUES (");
                    for (int column = 0; column < row.size(); column++) {
                        out.append(column == 0 ? "" : ", ").append(literal(row.get(column)));
                    }
                    out.append(");\n");
                }
                case UPDATE -> {
                    out.append("UPDATE ").append(table).append(" SET ");
                    boolean first = true;
                    for (int column = 0; column < row.size(); column++) {
                        if (column != keyIndex) {
                            out.append(first ? "" : ", ")
                                    .append(quotedColumns.get(column))
                                    .append(" = ")
                                    .append(literal(row.get(column)));
                            first = false;
                        }
                    }
                    appendWhere(row, out);
                }
                case DELETE -> {
                    out.append("DELETE FROM ").append(table);
                    appendWhere(row, out);
                }
            }
        }

        private void appendWhere(List<DataValue> row, StringBuilder out) {
            out.append(" WHERE ")
                    .append(quotedColumns.get(keyIndex))
                    .append(" = ")
                    .append(literal(row.get(keyIndex)))
                    .append(";\n");
        }

        /** Boolean Oracle ditulis 1/0: berlaku untuk NUMBER(1) dan BOOLEAN (23ai). JSON bersarang jadi string. */
        private String literal(DataValue value) {
            return switch (value.kind()) {
                case NULL -> "NULL";
                case NUMBER -> value.text();
                case BOOLEAN -> {
                    boolean flag = Boolean.parseBoolean(value.text());
                    if (options.target() == SyntaxTarget.ORACLE) {
                        yield flag ? "1" : "0";
                    }
                    yield flag ? "TRUE" : "FALSE";
                }
                case TEXT, JSON, RAW -> sqlString(value.text());
            };
        }
    }

    /** Collection lewat {@code db.getCollection(...)}: aman untuk nama yang bentrok dengan method {@code db}. */
    private static final class MongoStatements extends Statements {

        private final String collection;
        private final List<String> keys;

        MongoStatements(List<String> columns, GenerateOptions options, int keyIndex) {
            super(options, keyIndex);
            this.collection = "db.getCollection(" + JsonText.quote(options.table()) + ")";
            this.keys = columns.stream()
                    .map(name -> SIMPLE_IDENTIFIER.matcher(name).matches() ? name : JsonText.quote(name))
                    .toList();
        }

        @Override
        void writeRow(List<DataValue> row, int index, int total, StringBuilder out) {
            switch (options.operation()) {
                case INSERT -> {
                    if (index == 0) {
                        out.append(collection).append(".insertMany([\n");
                    }
                    out.append("  ").append(document(row, -1)).append(index == total - 1 ? "\n]);\n" : ",\n");
                }
                case UPDATE ->
                    out.append(collection)
                            .append(".updateOne(")
                            .append(filter(row))
                            .append(", { $set: ")
                            .append(document(row, keyIndex))
                            .append(" });\n");
                case DELETE ->
                    out.append(collection)
                            .append(".deleteOne(")
                            .append(filter(row))
                            .append(");\n");
            }
        }

        private String filter(List<DataValue> row) {
            return "{ " + keys.get(keyIndex) + ": " + literal(row.get(keyIndex)) + " }";
        }

        private String document(List<DataValue> row, int skipColumn) {
            StringBuilder document = new StringBuilder("{ ");
            boolean first = true;
            for (int column = 0; column < row.size(); column++) {
                if (column != skipColumn) {
                    document.append(first ? "" : ", ")
                            .append(keys.get(column))
                            .append(": ")
                            .append(literal(row.get(column)));
                    first = false;
                }
            }
            return document.append(" }").toString();
        }

        private static String literal(DataValue value) {
            return switch (value.kind()) {
                case NULL -> "null";
                case NUMBER, BOOLEAN, JSON -> value.text();
                case TEXT, RAW -> JsonText.quote(value.text());
            };
        }
    }
}
