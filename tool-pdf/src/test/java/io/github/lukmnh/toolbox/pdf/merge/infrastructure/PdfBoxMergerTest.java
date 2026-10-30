package io.github.lukmnh.toolbox.pdf.merge.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.pdf.merge.application.MergePdfUseCase;
import io.github.lukmnh.toolbox.pdf.merge.domain.MergeRequest;
import io.github.lukmnh.toolbox.pdf.merge.domain.MergeResult;
import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PasswordRequiredException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.infrastructure.PdfLoader;
import io.github.lukmnh.toolbox.pdf.testsupport.TestPdfs;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfBoxMergerTest {

    private final MergePdfUseCase useCase = new MergePdfUseCase(new PdfBoxMerger(new PdfLoader()));

    @TempDir
    Path temp;

    @Test
    void mergesAllPagesInOrder() throws IOException {
        Path first = TestPdfs.withNumberedPages(temp.resolve("a.pdf"), 2);
        Path second = TestPdfs.withNumberedPages(temp.resolve("b.pdf"), 3);
        Path output = temp.resolve("merged.pdf");

        MergeResult result = merge(List.of(PdfSource.of(second), PdfSource.of(first)), output);

        assertEquals(5, result.pageCount());
        assertEquals(5, TestPdfs.pageCount(output));
        String text = TestPdfs.extractText(output);
        // b.pdf (3 halaman) lebih dulu, lalu a.pdf (2 halaman)
        assertTrue(text.indexOf("Halaman 3") < text.lastIndexOf("Halaman 1"), text);
    }

    @Test
    void encryptedSourceNeedsPasswordAndMergesOnceGiven() throws IOException {
        Path plain = TestPdfs.withNumberedPages(temp.resolve("a.pdf"), 1);
        Path locked = TestPdfs.encrypted(temp.resolve("locked.pdf"), 2);
        Path output = temp.resolve("merged.pdf");

        assertThrows(
                PasswordRequiredException.class,
                () -> merge(List.of(PdfSource.of(plain), PdfSource.of(locked)), output));

        MergeResult result = merge(List.of(PdfSource.of(plain), new PdfSource(locked, TestPdfs.USER_PASSWORD)), output);
        assertEquals(3, result.pageCount());
        // Proteksi sumber terenkripsi ikut ke hasil gabungan
        assertTrue(TestPdfs.needsUserPassword(output));
    }

    private MergeResult merge(List<PdfSource> sources, Path output) {
        return useCase.execute(
                new MergeRequest(sources, output, OutputProtection.KEEP),
                ProgressReporter.NONE,
                CancellationToken.none());
    }
}
