package io.github.lukmnh.toolbox.productivity.rename.domain;

/** Status satu baris preview. Eksekusi hanya boleh bila tidak ada {@link #CONFLICT} maupun {@link #INVALID}. */
public enum RenameStatus {
    OK,
    UNCHANGED,
    CONFLICT,
    INVALID
}
