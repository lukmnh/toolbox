package io.github.lukmnh.toolbox.pdf.shared.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.pdf.shared.domain.CorruptPdfException;
import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PasswordRequiredException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfLimitExceededException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.testsupport.TestPdfs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
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
    void encryptedPdfKeepsPasswordAndPermissionsByDefault() throws IOException {
        AccessPermission noPrinting = new AccessPermission();
        noPrinting.setCanPrint(false);
        Path file = TestPdfs.encrypted(temp.resolve("rahasia.pdf"), 2, noPrinting);
        Path output = temp.resolve("hasil.pdf");

        save(new PdfSource(file, TestPdfs.USER_PASSWORD), output, OutputProtection.KEEP);

        assertThrows(
                InvalidPasswordException.class,
                () -> Loader.loadPDF(output.toFile()).close());
        try (PDDocument reopened = Loader.loadPDF(output.toFile(), TestPdfs.USER_PASSWORD)) {
            assertTrue(reopened.isEncrypted());
            assertFalse(reopened.getCurrentAccessPermission().canPrint());
            assertEquals(2, reopened.getNumberOfPages());
        }
    }

    @Test
    void removeProtectionSavesWithoutPassword() throws IOException {
        Path file = TestPdfs.encrypted(temp.resolve("rahasia.pdf"), 2);
        Path output = temp.resolve("terbuka.pdf");

        save(new PdfSource(file, TestPdfs.USER_PASSWORD), output, OutputProtection.REMOVE);

        try (PDDocument reopened = Loader.loadPDF(output.toFile())) {
            assertFalse(reopened.isEncrypted());
            assertEquals(2, reopened.getNumberOfPages());
        }
    }

    @Test
    void unencryptedPdfStaysUnencrypted() throws IOException {
        Path file = TestPdfs.withNumberedPages(temp.resolve("biasa.pdf"), 1);
        Path output = temp.resolve("hasil.pdf");

        save(PdfSource.of(file), output, OutputProtection.KEEP);

        try (PDDocument reopened = Loader.loadPDF(output.toFile())) {
            assertFalse(reopened.isEncrypted());
        }
    }

    private void save(PdfSource source, Path output, OutputProtection mode) throws IOException {
        try (PDDocument document = loader.load(source)) {
            loader.save(document, output, loader.protectionOf(document, source), mode);
        }
    }

    @Test
    void pdfWithTooManyPagesIsRejected() throws IOException {
        Path file = TestPdfs.blankPages(temp.resolve("tebal.pdf"), SecurityLimits.PDF_MAX_PAGES + 1);

        PdfLimitExceededException error =
                assertThrows(PdfLimitExceededException.class, () -> loader.load(PdfSource.of(file)));

        assertEquals(PdfLimitExceededException.Limit.PAGE_COUNT, error.limit());
        assertEquals(SecurityLimits.PDF_MAX_PAGES + 1, error.actual());
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
