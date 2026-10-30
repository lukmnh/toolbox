package io.github.lukmnh.toolbox.devutils.hash.domain;

/** Versi UUID yang bisa di-generate. */
public enum UuidVersion {
    /** Acak penuh (RFC 9562 §5.4). */
    V4,
    /** Timestamp Unix milidetik + acak; terurut waktu sehingga ramah index database (RFC 9562 §5.7). */
    V7
}
