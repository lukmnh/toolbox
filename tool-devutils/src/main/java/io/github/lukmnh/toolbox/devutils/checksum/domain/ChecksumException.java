package io.github.lukmnh.toolbox.devutils.checksum.domain;

import java.io.Serial;

/**
 * Semua error domain Checksum. Sealed supaya presentation bisa memetakan setiap jenis ke pesan user dengan
 * {@code switch} yang exhaustive.
 */
public abstract sealed class ChecksumException extends RuntimeException
        permits ChecksumIoException, InvalidChecksumException, InvalidSumsFileException, SumsFileTooLargeException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected ChecksumException(String message, Throwable cause) {
        super(message, cause);
    }
}
