package io.github.lukmnh.toolbox.devutils.sql.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlDialect;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ExportSqlUseCaseTest {

    private final FakeSqlFiles files = new FakeSqlFiles();
    private final ExportSqlUseCase useCase = new ExportSqlUseCase(files);

    @Test
    void writesMinifiedOutput() {
        String minified = new MinifySqlUseCase()
                .execute("select  a\nfrom t ;", SqlDialect.STANDARD, true, CancellationToken.none())
                .text();
        Path target = Path.of("query.min.sql");
        useCase.execute(minified, target);
        assertEquals("select a from t;", files.written.get(target));
    }

    @Test
    void emptyOutputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute("", Path.of("x.sql")));
    }
}
