package io.github.lukmnh.toolbox.devutils.jwt.domain;

/** Bagian token yang ditunjuk oleh pesan error. {@link #TOKEN} berarti struktur token secara keseluruhan. */
public enum JwtPart {
    TOKEN,
    HEADER,
    PAYLOAD,
    SIGNATURE
}
