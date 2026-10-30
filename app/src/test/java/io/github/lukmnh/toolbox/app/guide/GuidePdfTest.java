package io.github.lukmnh.toolbox.app.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.commonmark.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuidePdfTest {

    private static final String MARKDOWN = """
            # My Guide

            Intro with **bold** and `code`, see [docs](https://example.com).

            ## Lists

            - first
            - second
              1. nested one
              2. nested two

            3. three
            4. four

            ```
            mvn verify
            ```
            """;

    @TempDir
    Path directory;

    @Test
    void mapsMarkdownToPdfReport() throws Exception {
        Path target = directory.resolve("guide.pdf");

        new GuidePdf().write(MARKDOWN, "Fallback", target);

        try (PDDocument document = Loader.loadPDF(target.toFile())) {
            assertEquals("My Guide", document.getDocumentInformation().getTitle());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Intro with bold and code, see docs (https://example.com)."), text);
            assertTrue(text.contains("Lists"), text);
            assertTrue(text.contains("• first"), text);
            assertTrue(text.contains("• second"), text);
            assertTrue(text.contains("1. nested one"), text);
            assertTrue(text.contains("2. nested two"), text);
            assertTrue(text.contains("3. three"), text);
            assertTrue(text.contains("4. four"), text);
            assertTrue(text.contains("mvn verify"), text);
            assertFalse(text.contains("**"), text);
        }
    }

    @Test
    void fallbackTitleWhenGuideDoesNotStartWithH1() throws Exception {
        Path target = directory.resolve("guide.pdf");

        new GuidePdf().write("Just text.", "Fallback", target);

        try (PDDocument document = Loader.loadPDF(target.toFile())) {
            assertEquals("Fallback", document.getDocumentInformation().getTitle());
        }
    }

    @Test
    void inlineTextKeepsBareLinksOnce() {
        String text = GuidePdf.inline(Parser.builder().build().parse("<https://example.com>"));

        assertEquals("https://example.com", text);
    }

    @Test
    void bundledGuideExports() throws Exception {
        Path target = directory.resolve("bundled.pdf");

        new GuidePdf().write(GuideStore.defaultGuide(), "Fallback", target);

        try (PDDocument document = Loader.loadPDF(target.toFile())) {
            assertEquals("Toolbox Guide", document.getDocumentInformation().getTitle());
            assertTrue(document.getNumberOfPages() >= 1);
        }
    }
}
