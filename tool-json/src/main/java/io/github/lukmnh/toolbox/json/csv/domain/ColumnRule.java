package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.Objects;

/**
 * Aturan satu kolom di langkah Atur kolom.
 *
 * @param name nama baru; kosong berarti nama asal dipertahankan
 * @param keep {@code false} berarti kolom dihapus
 */
public record ColumnRule(String source, String name, boolean keep) {

    public ColumnRule {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(name, "name");
    }

    public static ColumnRule unchanged(String source) {
        return new ColumnRule(source, "", true);
    }

    public String effectiveName() {
        return name.isBlank() ? source : name.strip();
    }
}
