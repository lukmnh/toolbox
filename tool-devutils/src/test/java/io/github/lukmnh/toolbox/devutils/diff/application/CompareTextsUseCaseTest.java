package io.github.lukmnh.toolbox.devutils.diff.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.devutils.diff.domain.CompareRequest;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffOptions;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffResult;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffRow;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffStats;
import io.github.lukmnh.toolbox.devutils.diff.domain.RowType;
import io.github.lukmnh.toolbox.devutils.diff.domain.Segment;
import io.github.lukmnh.toolbox.devutils.diff.domain.WhitespaceMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompareTextsUseCaseTest {

    private final CompareTextsUseCase useCase = new CompareTextsUseCase(new LcsDiffer());

    private DiffResult compare(String oldText, String newText, DiffOptions options) {
        return useCase.execute(new CompareRequest(oldText, newText, "f.txt", options), CancellationToken.none());
    }

    private DiffResult compare(String oldText, String newText) {
        return compare(oldText, newText, DiffOptions.DEFAULT);
    }

    @Test
    void identicalTexts() {
        DiffResult result = compare("a\nb\n", "a\nb\n");
        assertTrue(result.stats().identical());
        assertTrue(result.unified().isEmpty());
        assertEquals(List.of(), result.changeStarts());
        assertEquals(2, result.rows().size());
    }

    @Test
    void onlyAdditions() {
        DiffResult result = compare("a\nb\n", "a\nx\ny\nb\n");
        assertEquals(new DiffStats(2, 0, 0), result.stats());
        DiffRow inserted = result.rows().get(1);
        assertEquals(RowType.INSERT, inserted.type());
        assertEquals(DiffRow.NO_LINE, inserted.oldNumber());
        assertEquals(2, inserted.newNumber());
        assertEquals(List.of(1), result.changeStarts());
    }

    @Test
    void onlyDeletions() {
        DiffResult result = compare("a\nx\nb\n", "a\nb\n");
        assertEquals(new DiffStats(0, 1, 0), result.stats());
        assertEquals(RowType.DELETE, result.rows().get(1).type());
        assertEquals(3, result.rows().get(2).oldNumber());
        assertEquals(2, result.rows().get(2).newNumber());
    }

    @Test
    void changeInTheMiddleIsPairedWithWordHighlight() {
        DiffResult result = compare("a\nint total = 1;\nb\n", "a\nint jumlah = 1;\nb\n");
        assertEquals(new DiffStats(0, 0, 1), result.stats());
        DiffRow changed = result.rows().get(1);
        assertEquals(RowType.CHANGE, changed.type());
        assertEquals(
                List.of(new Segment("int ", false), new Segment("total", true), new Segment(" = 1;", false)),
                changed.oldSegments());
        assertEquals(
                List.of(new Segment("int ", false), new Segment("jumlah", true), new Segment(" = 1;", false)),
                changed.newSegments());
    }

    @Test
    void unequalChangeBlockBecomesChangePlusInsert() {
        DiffResult result = compare("a\nold\nz\n", "a\nnew1\nnew2\nz\n");
        assertEquals(new DiffStats(1, 0, 1), result.stats());
        assertEquals(List.of(1), result.changeStarts());
    }

    @Test
    void ignoreOptionsTreatLinesAsEqual() {
        DiffOptions ignoreAll = new DiffOptions(WhitespaceMode.IGNORE_ALL, true, true);
        DiffResult result = compare("A = 1\r\nb\n", "a=1\nb\n", ignoreAll);
        assertTrue(result.stats().identical());
        // Sisi kanan tetap menampilkan teks baru apa adanya
        assertEquals("a=1", result.rows().getFirst().newSegments().getFirst().text());
    }

    @Test
    void lineEndingsDifferWhenNotNormalized() {
        DiffOptions strict = new DiffOptions(WhitespaceMode.EXACT, false, false);
        assertEquals(new DiffStats(0, 0, 1), compare("a\r\n", "a\n", strict).stats());
        assertTrue(compare("a\r\n", "a\n").stats().identical());
    }

    @Test
    void carriageReturnIsNotShownInRows() {
        DiffResult result = compare("a\r\nb\r\n", "a\r\nc\r\n");
        assertEquals("b", result.rows().get(1).oldSegments().getFirst().text());
        assertTrue(result.unified().text().contains("-b\r\n"));
    }

    @Test
    void cancelledTokenStopsTheComparison() {
        CancellationToken cancelled = new CancellationToken();
        cancelled.cancel();
        assertThrows(
                TaskCancelledException.class,
                () -> useCase.execute(new CompareRequest("a\n", "b\n", "f.txt", DiffOptions.DEFAULT), cancelled));
    }
}
