package io.github.lukmnh.toolbox.productivity.calculator.application;

import io.github.lukmnh.toolbox.productivity.calculator.domain.ReportDocument;
import io.github.lukmnh.toolbox.productivity.calculator.domain.ReportPdfWriter;
import java.nio.file.Path;

/** Tulis simulasi cicilan atau invoice yang sudah disusun sebagai PDF. Dijalankan di background task. */
public final class ExportPdfUseCase {

    private final ReportPdfWriter writer;

    public ExportPdfUseCase(ReportPdfWriter writer) {
        this.writer = writer;
    }

    public void execute(ReportDocument document, Path target) {
        writer.write(target, document);
    }
}
