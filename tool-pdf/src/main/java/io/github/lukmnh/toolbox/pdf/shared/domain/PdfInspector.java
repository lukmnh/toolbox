package io.github.lukmnh.toolbox.pdf.shared.domain;

/** Port: membaca informasi dasar file PDF. */
public interface PdfInspector {

    /**
     * @throws PasswordRequiredException bila file terenkripsi dan password kosong/salah
     * @throws CorruptPdfException bila file bukan PDF valid
     * @throws PdfIoException bila file tidak bisa dibaca
     */
    PdfDocumentInfo inspect(PdfSource source);
}
