package io.github.lukmnh.toolbox.pdf.sign.application;

import io.github.lukmnh.toolbox.pdf.sign.domain.EditSession;
import io.github.lukmnh.toolbox.pdf.sign.domain.FieldValue;

/** Mengisi satu field form. */
public final class UpdateFormFieldUseCase {

    /** @throws io.github.lukmnh.toolbox.pdf.shared.domain.FormFieldException bila field gagal diisi */
    public void execute(EditSession session, String fieldName, FieldValue value) {
        session.setFieldValue(fieldName, value);
    }
}
