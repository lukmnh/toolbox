package io.github.lukmnh.toolbox.devutils.mock.domain;

/** Tipe kolom mock. {@code defaultOptions} kosong = tipe tanpa opsi. */
public enum MockType {
    SEQUENCE(Kind.NUMBER, ""),
    UUID(Kind.TEXT, ""),
    NAME(Kind.TEXT, ""),
    EMAIL(Kind.TEXT, ""),
    PHONE(Kind.TEXT, ""),
    ADDRESS(Kind.TEXT, ""),
    CITY(Kind.TEXT, ""),
    INTEGER(Kind.NUMBER, "1..1000"),
    DECIMAL(Kind.NUMBER, "0..1000"),
    BOOLEAN(Kind.BOOLEAN, ""),
    DATE(Kind.TEXT, "2020-01-01..2026-12-31"),
    ENUM(Kind.TEXT, "active,inactive"),
    PATTERN(Kind.TEXT, "INV-####");

    /** Menentukan quoting: NUMBER/BOOLEAN ditulis tanpa kutip di JSON dan SQL. */
    public enum Kind {
        TEXT,
        NUMBER,
        BOOLEAN
    }

    private final Kind kind;
    private final String defaultOptions;

    MockType(Kind kind, String defaultOptions) {
        this.kind = kind;
        this.defaultOptions = defaultOptions;
    }

    public Kind kind() {
        return kind;
    }

    public String defaultOptions() {
        return defaultOptions;
    }

    public boolean hasOptions() {
        return !defaultOptions.isEmpty();
    }
}
