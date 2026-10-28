package io.github.lukmnh.toolbox.devutils.diff.domain;

/** Cara membandingkan spasi/tab saat menghitung diff. */
public enum WhitespaceMode {
    /** Spasi dibandingkan apa adanya. */
    EXACT,
    /** Semua whitespace diabaikan, misalnya {@code a = b} sama dengan {@code a=b}. */
    IGNORE_ALL,
    /** Hanya whitespace di akhir baris yang diabaikan. */
    IGNORE_TRAILING
}
