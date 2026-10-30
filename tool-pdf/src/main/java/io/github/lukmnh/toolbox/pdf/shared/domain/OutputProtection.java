package io.github.lukmnh.toolbox.pdf.shared.domain;

/** Perlakuan proteksi password PDF sumber pada file hasil (Security Requirements #2). */
public enum OutputProtection {
    /** Default: hasil memakai password dan permission yang sama dengan sumber. */
    KEEP,
    /** Hasil tanpa password; hanya setelah user memilih opsi ini secara eksplisit dan mengonfirmasi warning. */
    REMOVE
}
