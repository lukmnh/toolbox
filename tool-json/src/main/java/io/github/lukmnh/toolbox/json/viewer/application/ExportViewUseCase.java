package io.github.lukmnh.toolbox.json.viewer.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.json.viewer.domain.ExportFormat;
import io.github.lukmnh.toolbox.json.viewer.domain.ViewExporter;
import java.nio.file.Path;

/** Download hasil view (setelah filter/sort) sebagai JSON array, JSONL, atau CSV. */
public final class ExportViewUseCase {

    private final ViewExporter exporter;

    public ExportViewUseCase(ViewExporter exporter) {
        this.exporter = exporter;
    }

    /** @return {@code output} */
    public Path execute(
            ViewerSession session,
            int[] rows,
            ExportFormat format,
            Path output,
            ProgressReporter progress,
            CancellationToken cancellation) {
        exporter.export(session, rows, session.columns().data(), format, output, (done, total) -> {
            cancellation.throwIfCancelled();
            progress.report(done, total);
        });
        return output;
    }
}
