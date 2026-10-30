package io.github.lukmnh.toolbox.devutils.mock.domain;

import java.io.Serial;

/** Error domain Mock Data Generator; presentation memetakan tiap subtipe ke pesan. */
public abstract sealed class MockToolException extends RuntimeException
        permits InvalidMockSchemaException, MockIoException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected MockToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
