package io.github.lukmnh.toolbox.pdf.shared.domain;

import java.nio.file.Path;

/** Ringkasan file PDF untuk ditampilkan sebelum diproses. */
public record PdfDocumentInfo(Path file, int pageCount, long sizeBytes) {

    public PdfDocumentInfo {
        if (pageCount < 0) {
            throw new IllegalArgumentException("pageCount is negative: " + pageCount);
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes is negative: " + sizeBytes);
        }
    }
}
