package io.github.lukmnh.toolbox.pdf.split.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfLoader;
import io.github.lukmnh.toolbox.pdf.split.application.SplitPdfUseCase;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitMode;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitRequest;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitResult;
import io.github.lukmnh.toolbox.pdf.testsupport.TestPdfs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfBoxSplitterTest {

    private final SplitPdfUseCase useCase = new SplitPdfUseCase(new PdfBoxSplitter(new PdfLoader()));

    @TempDir
    Path temp;

    @Test
    void splitsRangesIntoSeparateFilesWithCorrectPages() throws IOException {
        Path source = TestPdfs.withNumberedPages(temp.resolve("laporan.pdf"), 6);
        Path out = Files.createDirectory(temp.resolve("out"));

        SplitResult result = split(PdfSource.of(source), new SplitMode.Ranges("2-4, 6"), out);

        assertEquals(2, result.files().size());
        Path first = out.resolve("laporan_hal2-4.pdf");
        Path second = out.resolve("laporan_hal6.pdf");
        assertEquals(3, TestPdfs.pageCount(first));
        assertEquals(1, TestPdfs.pageCount(second));
        String firstText = TestPdfs.extractText(first);
        assertTrue(firstText.contains("Halaman 2") && firstText.contains("Halaman 4"), firstText);
        assertTrue(TestPdfs.extractText(second).contains("Halaman 6"));
    }

    @Test
    void eachPageOfEncryptedPdfKeepsPasswordByDefault() throws IOException {
        Path source = TestPdfs.encrypted(temp.resolve("rahasia.pdf"), 3);
        Path out = Files.createDirectory(temp.resolve("out"));

        SplitResult result = split(new PdfSource(source, TestPdfs.USER_PASSWORD), new SplitMode.EachPage(), out);

        assertEquals(3, result.files().size());
        for (Path file : result.files()) {
            assertTrue(TestPdfs.needsUserPassword(file), file.getFileName().toString());
        }
    }

    @Test
    void encryptedPdfSplitWithRemoveProtectionProducesUnencryptedPages() throws IOException {
        Path source = TestPdfs.encrypted(temp.resolve("rahasia.pdf"), 3);
        Path out = Files.createDirectory(temp.resolve("out"));

        SplitResult result = split(
                new PdfSource(source, TestPdfs.USER_PASSWORD), new SplitMode.EachPage(), out, OutputProtection.REMOVE);

        for (Path file : result.files()) {
            assertEquals(1, TestPdfs.pageCount(file));
        }
    }

    private SplitResult split(PdfSource source, SplitMode mode, Path out) {
        return split(source, mode, out, OutputProtection.KEEP);
    }

    private SplitResult split(PdfSource source, SplitMode mode, Path out, OutputProtection protection) {
        String baseName = source.fileName().replace(".pdf", "");
        return useCase.execute(
                new SplitRequest(source, mode, out, baseName, protection),
                ProgressReporter.NONE,
                CancellationToken.none());
    }
}
