package io.github.lukmnh.toolbox.json.csv.domain;

/** Perlakuan baris yang jumlah kolomnya beda dari header (atau baris pertama bila tanpa header). */
public enum MismatchPolicy {
    /** Kolom kurang diisi kosong, kolom lebih dipotong. */
    PAD,
    /** Baris dilewati, tidak ikut ke hasil. */
    SKIP
}
