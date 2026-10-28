package io.github.lukmnh.toolbox.pdf.compress.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressRequest;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressResult;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressionPreset;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressionSession;
import io.github.lukmnh.toolbox.pdf.compress.domain.PdfCompressor;
import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CompressPdfUseCaseTest {

    private static final Path OUTPUT = Path.of("out.pdf");

    @Test
    void smallerResultIsKept() {
        FakeSession session = new FakeSession(1000, 400, true);

        CompressResult result = run(session);

        assertFalse(result.alreadyOptimal());
        assertEquals(400, result.resultSize());
        assertEquals(60.0, result.savingPercent(), 1e-9);
        assertFalse(session.copiedOriginal);
    }

    @Test
    void largerResultFallsBackToOriginalCopy() {
        FakeSession session = new FakeSession(1000, 1200, true);

        CompressResult result = run(session);

        assertTrue(result.alreadyOptimal());
        assertEquals(1000, result.resultSize());
        assertTrue(session.copiedOriginal);
    }

    @Test
    void nothingRecompressedSkipsSavingAndCopiesOriginal() {
        FakeSession session = new FakeSession(1000, 900, false);

        CompressResult result = run(session);

        assertTrue(result.alreadyOptimal());
        assertFalse(session.saved);
        assertTrue(session.copiedOriginal);
    }

    @Test
    void analyzesEveryPageBeforeRecompressing() {
        FakeSession session = new FakeSession(1000, 400, true);

        run(session);

        assertEquals(FakeSession.PAGES, session.analyzedPages);
        assertEquals(FakeSession.IMAGES, session.recompressCalls);
    }

    private static CompressResult run(FakeSession session) {
        PdfCompressor compressor = source -> session;
        CompressRequest request = new CompressRequest(
                PdfSource.of(Path.of("in.pdf")), CompressionPreset.DEFAULT, OUTPUT, OutputProtection.KEEP);
        return new CompressPdfUseCase(compressor).execute(request, ProgressReporter.NONE, CancellationToken.none());
    }

    private static final class FakeSession implements CompressionSession {
        static final int PAGES = 3;
        static final int IMAGES = 2;

        private final long originalSize;
        private final long savedSize;
        private final boolean recompresses;
        int analyzedPages;
        int recompressCalls;
        boolean saved;
        boolean copiedOriginal;

        FakeSession(long originalSize, long savedSize, boolean recompresses) {
            this.originalSize = originalSize;
            this.savedSize = savedSize;
            this.recompresses = recompresses;
        }

        @Override
        public int pageCount() {
            return PAGES;
        }

        @Override
        public long originalSize() {
            return originalSize;
        }

        @Override
        public void analyzePage(int pageIndex) {
            analyzedPages++;
        }

        @Override
        public int imageCount() {
            return IMAGES;
        }

        @Override
        public boolean recompressImage(int imageIndex, CompressionPreset preset) {
            recompressCalls++;
            return recompresses;
        }

        @Override
        public long save(Path output, OutputProtection protection) {
            saved = true;
            return savedSize;
        }

        @Override
        public long copyOriginal(Path output, OutputProtection protection) {
            copiedOriginal = true;
            return originalSize;
        }

        @Override
        public void close() {}
    }
}
