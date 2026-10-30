package io.github.lukmnh.toolbox.devutils.mock.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockFiles;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockFormat;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockGenerator;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockRenderer;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockSchema;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Export semua baris ke file secara streaming di background; batal + progress tiap {@code CHECKPOINT_ROWS}. */
public final class ExportMockUseCase {

    private final MockRenderer renderer;
    private final MockFiles files;

    public ExportMockUseCase(MockRenderer renderer, MockFiles files) {
        this.renderer = renderer;
        this.files = files;
    }

    /**
     * @throws io.github.lukmnh.toolbox.devutils.mock.domain.InvalidMockSchemaException bila schema tidak valid
     * @throws io.github.lukmnh.toolbox.devutils.mock.domain.MockIoException bila file gagal ditulis
     * @throws io.github.lukmnh.toolbox.common.task.TaskCancelledException bila dibatalkan user
     */
    public Path execute(
            MockSchema schema,
            MockFormat format,
            Path target,
            ProgressReporter progress,
            CancellationToken cancellation) {
        Iterator<List<String>> rows = MockGenerator.rows(schema);
        AtomicLong done = new AtomicLong();
        files.write(
                target,
                out -> renderer.render(schema, format, rows, schema.rows(), out, () -> {
                    cancellation.throwIfCancelled();
                    progress.report(done.addAndGet(MockRenderer.CHECKPOINT_ROWS), schema.rows());
                }));
        progress.report(schema.rows(), schema.rows());
        return target;
    }
}
