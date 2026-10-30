package io.github.lukmnh.toolbox.productivity.calculator.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.productivity.calculator.domain.PdfExportException;
import io.github.lukmnh.toolbox.productivity.calculator.domain.ReportDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfBoxReportWriterTest {

    @TempDir
    Path directory;

    @Test
    void rendersEveryBlock() throws IOException {
        SequencedMap<String, String> summary = new LinkedHashMap<>();
        summary.put("Grand total", "Rp115.920");
        ReportDocument document = ReportDocument.titled("Invoice INV-001")
                .heading("From (seller)")
                .paragraph("Toko Maju\nJakarta")
                .table(List.of("No", "Description", "Amount"), List.of(List.of("1", "Consulting", "Rp100.000")))
                .keyValues(summary)
                .build();
        Path target = directory.resolve("invoice.pdf");

        new PdfBoxReportWriter().write(target, document);

        try (PDDocument pdf = Loader.loadPDF(target.toFile())) {
            String text = new PDFTextStripper().getText(pdf);
            for (String expected :
                    List.of("Invoice INV-001", "From (seller)", "Toko Maju", "Jakarta", "Consulting", "Rp115.920")) {
                assertTrue(text.contains(expected), expected);
            }
        }
    }

    @Test
    void wrapsWriteFailureWithFileName() throws IOException {
        Path blocker = Files.createFile(directory.resolve("not-a-directory"));
        Path target = blocker.resolve("report.pdf");

        PdfExportException error = assertThrows(
                PdfExportException.class,
                () -> new PdfBoxReportWriter()
                        .write(target, ReportDocument.titled("x").build()));

        assertEquals("report.pdf", error.fileName());
    }

    @Test
    void tableRowsMustMatchHeaderWidth() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ReportDocument.titled("x").table(List.of("A", "B"), List.of(List.of("1"))));
    }
}
