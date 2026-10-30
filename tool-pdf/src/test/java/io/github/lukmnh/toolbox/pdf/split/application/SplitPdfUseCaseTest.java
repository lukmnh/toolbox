package io.github.lukmnh.toolbox.pdf.split.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.pdf.shared.domain.InvalidPageRangeException;
import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PageRange;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.split.domain.PdfSplitter;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitMode;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitRequest;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitResult;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitSession;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SplitPdfUseCaseTest {

    private static final Path OUT = Path.of("out");

    private final FakeSplitter splitter = new FakeSplitter(10);
    private final SplitPdfUseCase useCase = new SplitPdfUseCase(splitter);

    @Test
    void eachPageModeWritesOneFilePerPage() {
        splitter.pageCount = 3;

        SplitResult result = split(new SplitMode.EachPage());

        assertEquals(
                List.of(
                        OUT.resolve("laporan_hal1.pdf"),
                        OUT.resolve("laporan_hal2.pdf"),
                        OUT.resolve("laporan_hal3.pdf")),
                result.files());
    }

    @Test
    void rangeModeWritesOneFilePerRangeWithRangeInName() {
        split(new SplitMode.Ranges("1-3, 5, 8-10"));

        assertEquals(
                Map.of(
                        "laporan_hal1-3.pdf", new PageRange(1, 3),
                        "laporan_hal5.pdf", PageRange.single(5),
                        "laporan_hal8-10.pdf", new PageRange(8, 10)),
                splitter.session.extracted);
    }

    @Test
    void duplicateRangesDoNotOverwriteEachOther() {
        SplitResult result = split(new SplitMode.Ranges("2, 2"));

        assertEquals(List.of(OUT.resolve("laporan_hal2.pdf"), OUT.resolve("laporan_hal2_2.pdf")), result.files());
    }

    @Test
    void invalidRangeIsValidatedAgainstActualPageCountAndClosesSession() {
        assertThrows(InvalidPageRangeException.class, () -> split(new SplitMode.Ranges("9-12")));
        assertTrue(splitter.session.closed);
    }

    private SplitResult split(SplitMode mode) {
        SplitRequest request =
                new SplitRequest(PdfSource.of(Path.of("laporan.pdf")), mode, OUT, "laporan", OutputProtection.KEEP);
        return useCase.execute(request, ProgressReporter.NONE, CancellationToken.none());
    }

    private static final class FakeSplitter implements PdfSplitter {
        int pageCount;
        FakeSession session;

        FakeSplitter(int pageCount) {
            this.pageCount = pageCount;
        }

        @Override
        public SplitSession open(PdfSource source) {
            session = new FakeSession(pageCount);
            return session;
        }
    }

    private static final class FakeSession implements SplitSession {
        final Map<String, PageRange> extracted = new LinkedHashMap<>();
        final int pageCount;
        boolean closed;

        FakeSession(int pageCount) {
            this.pageCount = pageCount;
        }

        @Override
        public int pageCount() {
            return pageCount;
        }

        @Override
        public void extract(PageRange range, Path output, OutputProtection protection) {
            extracted.put(output.getFileName().toString(), range);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
