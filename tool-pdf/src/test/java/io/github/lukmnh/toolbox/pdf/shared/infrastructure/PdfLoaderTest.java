package io.github.lukmnh.toolbox.pdf.shared.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.pdf.shared.domain.CorruptPdfException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PasswordRequiredException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.testsupport.TestPdfs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfLoaderTest {

    private final PdfLoader loader = new PdfLoader();

    @TempDir
    Path temp;

    @Test
    void encryptedPdfWithoutPasswordAsksForPassword() throws IOException {
        Path file = TestPdfs.encrypted(temp.resolve("rahasia.pdf"), 1);

        PasswordRequiredException error =
                assertThrows(PasswordRequiredException.class, () -> loader.load(PdfSource.of(file)));

        assertFalse(error.passwordWasGiven());
        assertEquals(file, error.file());
    }

    @Test
    void wrongPasswordIsReportedAsWrong() throws IOException {
        Path file = TestPdfs.encrypted(temp.resolve("rahasia.pdf"), 1);

        PasswordRequiredException error =
                assertThrows(PasswordRequiredException.class, () -> loader.load(new PdfSource(file, "salah")));

        assertTrue(error.passwordWasGiven());
    }

    @Test
    void savedCopyOfEncryptedPdfNoLongerNeedsPassword() throws IOException {
        Path file = TestPdfs.encrypted(temp.resolve("rahasia.pdf"), 2);
        Path output = temp.resolve("terbuka.pdf");

        try (PDDocument document = loader.load(new PdfSource(file, TestPdfs.USER_PASSWORD))) {
            loader.save(document, output);
        }

        try (PDDocument reopened = Loader.loadPDF(output.toFile())) {
            assertFalse(reopened.isEncrypted());
            assertEquals(2, reopened.getNumberOfPages());
        }
    }

    @Test
    void nonPdfIsReportedAsCorrupt() throws IOException {
        Path file = Files.writeString(temp.resolve("bukan.pdf"), "ini bukan pdf");

        assertThrows(CorruptPdfException.class, () -> loader.load(PdfSource.of(file)));
    }

    @Test
    void missingFileIsReportedAsIoError() {
        assertThrows(PdfIoException.class, () -> loader.load(PdfSource.of(temp.resolve("tidak-ada.pdf"))));
    }

    @Test
    void sourceFileCanBeDeletedWhileDocumentIsOpen() throws IOException {
        // Di Windows, file yang masih dibuka tidak bisa dihapus/ditimpa. Load dari bytes mencegah lock itu.
        Path file = TestPdfs.withNumberedPages(temp.resolve("a.pdf"), 1);

        try (PDDocument document = loader.load(PdfSource.of(file))) {
            Files.delete(file);
            assertEquals(1, document.getNumberOfPages());
        }
    }
}
