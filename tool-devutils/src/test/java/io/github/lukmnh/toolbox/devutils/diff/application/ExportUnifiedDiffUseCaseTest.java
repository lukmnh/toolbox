package io.github.lukmnh.toolbox.devutils.diff.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.devutils.diff.domain.UnifiedDiff;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExportUnifiedDiffUseCaseTest {

    private final FakeTextFiles files = new FakeTextFiles();
    private final ExportUnifiedDiffUseCase useCase = new ExportUnifiedDiffUseCase(files);

    @Test
    void writesDiffTextWithTrailingNewline() {
        Path target = Path.of("hasil.diff");
        useCase.execute(new UnifiedDiff(List.of("--- a/f", "+++ b/f", "@@ -1 +1 @@")), target);
        assertEquals("--- a/f\n+++ b/f\n@@ -1 +1 @@\n", files.written.get(target));
    }

    @Test
    void emptyDiffIsRejected() {
        assertThrows(
                IllegalArgumentException.class, () -> useCase.execute(new UnifiedDiff(List.of()), Path.of("x.diff")));
    }
}
