package io.github.lukmnh.toolbox.pdf.shared.application;

import io.github.lukmnh.toolbox.pdf.shared.domain.PdfDocumentInfo;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfInspector;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;

/** Membaca jumlah halaman dan ukuran file sebelum diproses. */
public final class InspectPdfUseCase {

    private final PdfInspector inspector;

    public InspectPdfUseCase(PdfInspector inspector) {
        this.inspector = inspector;
    }

    public PdfDocumentInfo execute(PdfSource source) {
        return inspector.inspect(source);
    }
}
