package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.io.Serial;

/** Error domain Regex Tester; sealed supaya pemetaan pesan di presentation exhaustive. */
public abstract sealed class RegexToolException extends RuntimeException
        permits RegexCompileException,
                UnsupportedRegexException,
                InvalidReplacementException,
                TestTextTooLargeException,
                RegexIoException,
                RegexTooComplexException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected RegexToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
