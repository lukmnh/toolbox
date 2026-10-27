package io.github.lukmnh.toolbox.pdf.sign.application;

import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.sign.domain.EditSession;
import io.github.lukmnh.toolbox.pdf.sign.domain.PdfEditor;

/** Membuka PDF untuk diedit. Pemanggil memiliki session dan wajib menutupnya. */
public final class OpenDocumentUseCase {

    private final PdfEditor editor;

    public OpenDocumentUseCase(PdfEditor editor) {
        this.editor = editor;
    }

    public EditSession execute(PdfSource source) {
        return editor.open(source);
    }
}
