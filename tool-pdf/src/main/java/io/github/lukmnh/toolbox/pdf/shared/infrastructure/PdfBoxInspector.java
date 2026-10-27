package io.github.lukmnh.toolbox.pdf.shared.infrastructure;

import io.github.lukmnh.toolbox.pdf.shared.domain.PdfDocumentInfo;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfInspector;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;

public final class PdfBoxInspector implements PdfInspector {

    private final PdfLoader loader;

    public PdfBoxInspector(PdfLoader loader) {
        this.loader = loader;
    }

    @Override
    public PdfDocumentInfo inspect(PdfSource source) {
        try (PDDocument document = loader.load(source)) {
            return new PdfDocumentInfo(source.file(), document.getNumberOfPages(), loader.sizeOf(source.file()));
        } catch (IOException e) {
            throw new PdfIoException(source.file(), e);
        }
    }
}
