package io.github.lukmnh.toolbox.pdf.merge.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.pdf.merge.domain.MergeRequest;
import io.github.lukmnh.toolbox.pdf.merge.domain.MergeResult;
import io.github.lukmnh.toolbox.pdf.merge.domain.MergeSession;
import io.github.lukmnh.toolbox.pdf.merge.domain.PdfMerger;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.util.List;

/** Menggabungkan beberapa PDF menjadi satu file. */
public final class MergePdfUseCase {

    private final PdfMerger merger;

    public MergePdfUseCase(PdfMerger merger) {
        this.merger = merger;
    }

    /**
     * @throws io.github.lukmnh.toolbox.pdf.shared.domain.PdfToolException bila salah satu file gagal dibaca
     * @throws io.github.lukmnh.toolbox.common.task.TaskCancelledException bila dibatalkan
     */
    public MergeResult execute(MergeRequest request, ProgressReporter progress, CancellationToken cancellation) {
        List<PdfSource> sources = request.sources();
        // Langkah terakhir (simpan) dihitung sebagai satu unit progress
        int totalSteps = sources.size() + 1;
        try (MergeSession session = merger.openSession()) {
            for (int index = 0; index < sources.size(); index++) {
                cancellation.throwIfCancelled();
                session.append(sources.get(index));
                progress.report(index + 1L, totalSteps);
            }
            cancellation.throwIfCancelled();
            session.save(request.output(), request.protection());
            progress.report(totalSteps, totalSteps);
            return new MergeResult(request.output(), sources.size(), session.pageCount());
        }
    }
}
