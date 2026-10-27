package io.github.lukmnh.toolbox.pdf.shared.domain;

import java.io.Serial;

/** Gagal mengisi field form, biasanya karena appearance atau font field rusak. */
public final class FormFieldException extends PdfToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fieldName;

    public FormFieldException(String fieldName, Throwable cause) {
        super("Gagal mengisi field '" + fieldName + "'", cause);
        this.fieldName = fieldName;
    }

    public String fieldName() {
        return fieldName;
    }
}
