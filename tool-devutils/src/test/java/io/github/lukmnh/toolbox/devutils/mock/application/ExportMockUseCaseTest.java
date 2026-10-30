package io.github.lukmnh.toolbox.devutils.mock.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockColumn;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockFiles;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockFormat;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockLocale;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockRenderer;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockSchema;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class ExportMockUseCaseTest {

    private static final int ROWS = 2_500;
    private static final Path TARGET = Path.of("mock.sql");

    /** Port palsu: menulis ke memori supaya test tidak bergantung ke infrastructure. */
    private final StringBuilder written = new StringBuilder();

    private final MockFiles files = (target, content) -> {
        try {
            content.writeTo(written);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    };
    private final ExportMockUseCase export = new ExportMockUseCase(new MockRenderer(UnaryOperator.identity()), files);

    @Test
    void writesEveryRowAndReportsCompletion() {
        long[] lastProgress = new long[2];
        ProgressReporter progress = (done, total) -> {
            lastProgress[0] = done;
            lastProgress[1] = total;
        };

        export.execute(schema(), MockFormat.SQL, TARGET, progress, CancellationToken.none());

        assertEquals(ROWS, written.toString().lines().count());
        assertEquals(ROWS, lastProgress[0]);
        assertEquals(ROWS, lastProgress[1]);
    }

    @Test
    void cancellationStopsAtNextCheckpoint() {
        CancellationToken cancellation = new CancellationToken();
        cancellation.cancel();

        assertThrows(
                TaskCancelledException.class,
                () -> export.execute(schema(), MockFormat.SQL, TARGET, ProgressReporter.NONE, cancellation));
        assertEquals(MockRenderer.CHECKPOINT_ROWS, written.toString().lines().count());
    }

    private static MockSchema schema() {
        return new MockSchema(
                List.of(new MockColumn("id", MockType.SEQUENCE, ""), new MockColumn("name", MockType.NAME, "")),
                MockLocale.ID,
                5,
                ROWS,
                "users");
    }
}
