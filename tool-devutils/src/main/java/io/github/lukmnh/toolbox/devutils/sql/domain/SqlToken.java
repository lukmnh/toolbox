package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.util.Objects;

/** Potongan {@code [start, end)} dari teks SQL; offset saja supaya file 10 MB tidak menggandakan memori. */
public record SqlToken(SqlTokenType type, int start, int end) {

    public SqlToken {
        Objects.requireNonNull(type, "type");
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Rentang token tidak valid: " + start + ".." + end);
        }
    }

    public String text(CharSequence sql) {
        return sql.subSequence(start, end).toString();
    }
}
