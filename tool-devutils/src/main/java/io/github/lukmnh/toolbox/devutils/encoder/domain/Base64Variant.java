package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.util.Base64;

/** Varian alfabet/format Base64 (RFC 4648 dan RFC 2045). */
public enum Base64Variant {
    STANDARD,
    URL_SAFE,
    /** Dibungkus 76 kolom dengan CRLF, untuk email/MIME. */
    MIME;

    public Base64.Encoder encoder(boolean padding) {
        Base64.Encoder encoder = switch (this) {
            case STANDARD -> Base64.getEncoder();
            case URL_SAFE -> Base64.getUrlEncoder();
            case MIME -> Base64.getMimeEncoder();
        };
        return padding ? encoder : encoder.withoutPadding();
    }
}
