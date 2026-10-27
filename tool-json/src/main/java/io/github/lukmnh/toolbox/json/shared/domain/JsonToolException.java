package io.github.lukmnh.toolbox.json.shared.domain;

import java.io.Serial;

/**
 * Semua error domain tool JSON. Sealed supaya presentation bisa memetakan setiap jenis ke pesan user
 * dengan {@code switch} yang exhaustive.
 */
public abstract sealed class JsonToolException extends RuntimeException
        permits MalformedJsonException, JsonIoException, NotJsonFileException, InvalidQueryException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected JsonToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
