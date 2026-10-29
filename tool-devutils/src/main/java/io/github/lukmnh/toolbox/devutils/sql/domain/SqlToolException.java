package io.github.lukmnh.toolbox.devutils.sql.domain;

import java.io.Serial;

/** Error domain SQL Formatter; sealed supaya pemetaan pesan di presentation exhaustive. */
public abstract sealed class SqlToolException extends RuntimeException
        permits SqlFileTooLargeException, SqlIoException, SqlFormatFailedException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected SqlToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
