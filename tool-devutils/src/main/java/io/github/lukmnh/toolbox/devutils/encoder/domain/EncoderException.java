package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.io.Serial;

/**
 * Semua error domain Encoder. Sealed supaya presentation bisa memetakan setiap jenis ke pesan user dengan
 * {@code switch} yang exhaustive.
 */
public abstract sealed class EncoderException extends RuntimeException
        permits InvalidBase64Exception, InvalidPercentEncodingException, EncoderIoException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected EncoderException(String message, Throwable cause) {
        super(message, cause);
    }
}
