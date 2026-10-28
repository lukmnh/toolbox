package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.io.Serial;

/**
 * Semua error domain JWT Debugger. Sealed supaya presentation bisa memetakan setiap jenis ke pesan user dengan
 * {@code switch} yang exhaustive. Pesan exception tidak pernah berisi token atau secret (Security Requirements #5).
 */
public abstract sealed class JwtToolException extends RuntimeException
        permits MalformedJwtException,
                JweNotSupportedException,
                UnsecuredJwtException,
                UnsupportedAlgorithmException,
                KeyMismatchException,
                UnreadableKeyException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected JwtToolException(String message) {
        super(message);
    }

    protected JwtToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
