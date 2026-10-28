package io.github.lukmnh.toolbox.devutils.diff.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.devutils.diff.domain.CompareRequest;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffChunk;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffResult;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffRow;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffRowsBuilder;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffSummary;
import io.github.lukmnh.toolbox.devutils.diff.domain.LineKey;
import io.github.lukmnh.toolbox.devutils.diff.domain.SequenceDiffer;
import io.github.lukmnh.toolbox.devutils.diff.domain.TextLines;
import io.github.lukmnh.toolbox.devutils.diff.domain.UnifiedDiff;
import io.github.lukmnh.toolbox.devutils.diff.domain.UnifiedDiffWriter;
import java.util.List;

/** Bandingkan dua teks per baris (Myers), lengkap dengan highlight per kata dan unified diff. */
public final class CompareTextsUseCase {

    private final SequenceDiffer differ;

    public CompareTextsUseCase(SequenceDiffer differ) {
        this.differ = differ;
    }

    /** @throws io.github.lukmnh.toolbox.common.task.TaskCancelledException bila dibatalkan */
    public DiffResult execute(CompareRequest request, CancellationToken cancellation) {
        Runnable checkpoint = cancellation::throwIfCancelled;
        TextLines oldText = TextLines.of(request.oldText());
        TextLines newText = TextLines.of(request.newText());
        List<DiffChunk> chunks = differ.diff(
                LineKey.ofLines(oldText, request.options()), LineKey.ofLines(newText, request.options()), checkpoint);
        List<DiffRow> rows = new DiffRowsBuilder(differ, request.options(), checkpoint).build(oldText, newText, chunks);
        UnifiedDiff unified = UnifiedDiffWriter.write(
                request.fileName(), oldText, newText, chunks, UnifiedDiffWriter.DEFAULT_CONTEXT);
        return new DiffResult(rows, DiffSummary.stats(rows), DiffSummary.changeStarts(rows), unified);
    }
}
