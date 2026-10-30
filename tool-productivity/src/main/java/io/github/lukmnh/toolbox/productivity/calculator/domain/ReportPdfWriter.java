package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.nio.file.Path;

/** Port penulis PDF (library PDF ada di luar batas domain). */
public interface ReportPdfWriter {

    /** @throws PdfExportException bila gagal ditulis */
    void write(Path target, ReportDocument document);
}
