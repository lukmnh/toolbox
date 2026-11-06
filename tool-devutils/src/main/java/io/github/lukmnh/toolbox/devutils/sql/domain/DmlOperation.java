package io.github.lukmnh.toolbox.devutils.sql.domain;

/** Jenis statement yang di-generate. */
public enum DmlOperation {
    INSERT,
    UPDATE,
    DELETE;

    /** UPDATE/DELETE butuh kolom key untuk WHERE / filter. */
    public boolean needsKey() {
        return this != INSERT;
    }
}
