package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.Objects;

/**
 * @param table nama tabel ({@code schema.table} boleh) atau collection MongoDB
 * @param keyColumn kolom untuk WHERE (UPDATE/DELETE); {@code null} untuk INSERT
 * @param allText semua nilai non-NULL ditulis sebagai teks (mis. kolom VARCHAR berisi angka)
 */
public record GenerateOptions(
        SyntaxTarget target, DmlOperation operation, String table, String keyColumn, boolean allText) {

    public GenerateOptions {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(table, "table");
        table = table.strip();
        if (table.isEmpty()) {
            throw new IllegalArgumentException("Table name is blank");
        }
        if (operation.needsKey() && keyColumn == null) {
            throw new IllegalArgumentException(operation + " needs a key column");
        }
    }
}
