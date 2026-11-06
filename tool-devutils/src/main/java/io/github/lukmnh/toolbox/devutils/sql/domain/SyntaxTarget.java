package io.github.lukmnh.toolbox.devutils.sql.domain;

/** Database tujuan SQL Generator. Urutan enum = urutan tampil. */
public enum SyntaxTarget {
    POSTGRESQL("PostgreSQL", "sql"),
    ORACLE("Oracle", "sql"),
    MONGODB("MongoDB", "js");

    private final String displayName;
    private final String extension;

    SyntaxTarget(String displayName, String extension) {
        this.displayName = displayName;
        this.extension = extension;
    }

    /** Nama produk, tidak diterjemahkan. */
    public String displayName() {
        return displayName;
    }

    /** Ekstensi file hasil tanpa titik. */
    public String extension() {
        return extension;
    }
}
