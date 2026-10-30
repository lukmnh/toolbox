package io.github.lukmnh.toolbox.devutils.diff.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.devutils.diff.application.CompareTextsUseCase;
import io.github.lukmnh.toolbox.devutils.diff.domain.ChunkType;
import io.github.lukmnh.toolbox.devutils.diff.domain.CompareRequest;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffChunk;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffOptions;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffResult;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffStats;
import io.github.lukmnh.toolbox.devutils.diff.domain.TextLines;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class JavaDiffUtilsDifferTest {

    private final JavaDiffUtilsDiffer differ = new JavaDiffUtilsDiffer();
    private final CompareTextsUseCase useCase = new CompareTextsUseCase(differ);

    private static String lines(int count, String prefix) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> prefix + i).collect(Collectors.joining("\n")) + "\n";
    }

    @Test
    void replacedBlockIsReportedAsOneChangeChunk() {
        List<DiffChunk> chunks = differ.diff(List.of("a", "b", "c"), List.of("a", "x", "c"), () -> {});
        assertEquals(List.of(new DiffChunk(ChunkType.CHANGE, 1, 2, 1, 2)), chunks);
    }

    @Test
    void insertAndDeleteChunks() {
        assertEquals(
                List.of(new DiffChunk(ChunkType.INSERT, 1, 1, 1, 2)),
                differ.diff(List.of("a", "b"), List.of("a", "x", "b"), () -> {}));
        assertEquals(
                List.of(new DiffChunk(ChunkType.DELETE, 0, 1, 0, 0)),
                differ.diff(List.of("a", "b"), List.of("b"), () -> {}));
    }

    @Test
    void unifiedDiffRoundTripsThroughPatch() throws PatchFailedException {
        String oldText = lines(200, "baris ");
        String newText = oldText.replace("baris 10\n", "baris sepuluh\n")
                .replace("baris 50\n", "")
                .replace("baris 120\n", "baris 120\nsisipan A\nsisipan B\n");
        DiffResult result = useCase.execute(
                new CompareRequest(oldText, newText, "f.txt", DiffOptions.DEFAULT), CancellationToken.none());
        assertEquals(new DiffStats(2, 1, 1), result.stats());

        Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(result.unified().lines());
        List<String> patched = patch.applyTo(TextLines.of(oldText).lines());
        assertEquals(TextLines.of(newText).lines(), patched);
    }

    @Test
    void largeIdenticalFilesAreCheap() {
        String text = lines(100_000, "line ");
        DiffResult result =
                useCase.execute(new CompareRequest(text, text, "f.txt", DiffOptions.DEFAULT), CancellationToken.none());
        assertEquals(100_000, result.rows().size());
        assertEquals(0, result.changeStarts().size());
    }

    @Test
    void cancellationInterruptsTheAlgorithm() {
        List<String> original = new ArrayList<>();
        List<String> revised = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) {
            original.add("a" + i);
            revised.add("b" + i);
        }
        assertThrows(
                TaskCancelledException.class,
                () -> differ.diff(original, revised, () -> {
                    throw new TaskCancelledException();
                }));
    }
}
