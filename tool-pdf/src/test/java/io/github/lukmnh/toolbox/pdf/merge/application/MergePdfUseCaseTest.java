package io.github.lukmnh.toolbox.pdf.merge.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.pdf.merge.domain.MergeRequest;
import io.github.lukmnh.toolbox.pdf.merge.domain.MergeResult;
import io.github.lukmnh.toolbox.pdf.merge.domain.MergeSession;
import io.github.lukmnh.toolbox.pdf.merge.domain.PdfMerger;
import io.github.lukmnh.toolbox.pdf.shared.domain.OutputProtection;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MergePdfUseCaseTest {

    private static final Path OUTPUT = Path.of("merged.pdf");

    private final FakeMerger merger = new FakeMerger();
    private final MergePdfUseCase useCase = new MergePdfUseCase(merger);

    @Test
    void appendsSourcesInRequestedOrderThenSaves() {
        List<PdfSource> sources = List.of(source("b.pdf"), source("a.pdf"), source("c.pdf"));

        MergeResult result = useCase.execute(
                new MergeRequest(sources, OUTPUT, OutputProtection.KEEP),
                ProgressReporter.NONE,
                CancellationToken.none());

        assertEquals(List.of("b.pdf", "a.pdf", "c.pdf"), merger.session.appended);
        assertEquals(OUTPUT, merger.session.savedTo);
        assertEquals(3, result.fileCount());
        assertEquals(3 * FakeMerger.PAGES_PER_FILE, result.pageCount());
        assertTrue(merger.session.closed);
    }

    @Test
    void reportsProgressUpToCompletion() {
        List<long[]> reports = new ArrayList<>();

        useCase.execute(
                new MergeRequest(List.of(source("a.pdf"), source("b.pdf")), OUTPUT, OutputProtection.KEEP),
                (done, total) -> reports.add(new long[] {done, total}),
                CancellationToken.none());

        long[] last = reports.getLast();
        assertEquals(last[1], last[0]);
        assertEquals(3, reports.size());
    }

    @Test
    void cancellationStopsBeforeSavingAndClosesSession() {
        CancellationToken token = new CancellationToken();
        token.cancel();

        assertThrows(
                TaskCancelledException.class,
                () -> useCase.execute(
                        new MergeRequest(List.of(source("a.pdf"), source("b.pdf")), OUTPUT, OutputProtection.KEEP),
                        ProgressReporter.NONE,
                        token));

        assertFalse(merger.session.saved());
        assertTrue(merger.session.closed);
    }

    @Test
    void requestNeedsAtLeastTwoFiles() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MergeRequest(List.of(source("a.pdf")), OUTPUT, OutputProtection.KEEP));
    }

    private static PdfSource source(String name) {
        return PdfSource.of(Path.of(name));
    }

    private static final class FakeMerger implements PdfMerger {
        static final int PAGES_PER_FILE = 2;
        final FakeSession session = new FakeSession();

        @Override
        public MergeSession openSession() {
            return session;
        }
    }

    private static final class FakeSession implements MergeSession {
        final List<String> appended = new ArrayList<>();
        Path savedTo;
        boolean closed;

        @Override
        public void append(PdfSource source) {
            appended.add(source.fileName());
        }

        @Override
        public int pageCount() {
            return appended.size() * FakeMerger.PAGES_PER_FILE;
        }

        @Override
        public void save(Path output, OutputProtection protection) {
            savedTo = output;
        }

        boolean saved() {
            return savedTo != null;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
