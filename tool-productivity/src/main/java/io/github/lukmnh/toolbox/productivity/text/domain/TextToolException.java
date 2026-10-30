package io.github.lukmnh.toolbox.productivity.text.domain;

import java.io.Serial;

/** Error domain Text Tools; sealed supaya pemetaan pesan di presentation exhaustive. */
public abstract sealed class TextToolException extends RuntimeException
        permits TextFileTooLargeException, TextIoException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected TextToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
