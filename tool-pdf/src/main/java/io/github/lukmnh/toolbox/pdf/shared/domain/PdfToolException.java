package io.github.lukmnh.toolbox.pdf.shared.domain;

import java.io.Serial;

/**
 * Semua error domain tool PDF. Sealed supaya presentation bisa memetakan setiap jenis ke pesan user
 * dengan {@code switch} yang exhaustive.
 */
public abstract sealed class PdfToolException extends RuntimeException
        permits PasswordRequiredException,
                CorruptPdfException,
                PdfIoException,
                InvalidPageRangeException,
                FormFieldException,
                UnsupportedTextException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected PdfToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
