package io.github.lukmnh.toolbox.common.report;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfReportTest {

    @TempDir
    Path directory;

    @Test
    void writesAllBlocksAndRoundTripsAttachment() throws IOException {
        Path pdf = directory.resolve("report.pdf");
        LinkedHashMap<String, String> summary = new LinkedHashMap<>();
        summary.put("Monthly payment", "Rp 1.250.000");
        byte[] data = "{\"name\":\"Budget\"}".getBytes(StandardCharsets.UTF_8);

        PdfReport.titled("Installment schedule")
                .heading("Summary")
                .keyValues(summary)
                .paragraph("Unsupported glyph → replaced")
                .table(List.of("Month", "Principal"), List.of(List.of("1", "1.000.000"), List.of("2", "1.000.000")))
                .attach("budget.json", data)
                .writeTo(pdf);

        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Installment schedule"));
            assertTrue(text.contains("Rp 1.250.000"));
            assertTrue(text.contains("Unsupported glyph ? replaced"));
        }
        assertArrayEquals(
                data, PdfReport.readAttachment(pdf, "budget.json", 1024).orElseThrow());
        assertEquals(Optional.empty(), PdfReport.readAttachment(pdf, "other.json", 1024));
        assertThrows(IOException.class, () -> PdfReport.readAttachment(pdf, "budget.json", 4));
    }

    @Test
    void breaksLongTablesAcrossPages() throws IOException {
        Path pdf = directory.resolve("long.pdf");
        List<List<String>> rows = Collections.nCopies(200, List.of("row", "123"));

        PdfReport.titled("Long").table(List.of("A", "B"), rows).writeTo(pdf);

        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            assertTrue(document.getNumberOfPages() > 1);
        }
    }
}
