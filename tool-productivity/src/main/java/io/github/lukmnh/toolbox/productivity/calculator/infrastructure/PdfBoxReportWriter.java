package io.github.lukmnh.toolbox.productivity.calculator.infrastructure;

import io.github.lukmnh.toolbox.common.report.PdfReport;
import io.github.lukmnh.toolbox.productivity.calculator.domain.PdfExportException;
import io.github.lukmnh.toolbox.productivity.calculator.domain.ReportDocument;
import io.github.lukmnh.toolbox.productivity.calculator.domain.ReportPdfWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/** Render {@link ReportDocument} lewat {@link PdfReport} bersama (PDFBox, A4, Helvetica). */
public final class PdfBoxReportWriter implements ReportPdfWriter {

    @Override
    public void write(Path target, ReportDocument document) {
        PdfReport report = PdfReport.titled(document.title());
        for (ReportDocument.Block block : document.blocks()) {
            switch (block) {
                case ReportDocument.Heading heading -> report.heading(heading.text());
                case ReportDocument.Paragraph paragraph -> report.paragraph(paragraph.text());
                case ReportDocument.KeyValues keyValues -> report.keyValues(keyValues.rows());
                case ReportDocument.Table table -> report.table(table.headers(), table.rows());
            }
        }
        try {
            report.writeTo(target);
        } catch (IOException | UncheckedIOException e) {
            throw new PdfExportException(target.getFileName().toString(), e);
        }
    }
}
