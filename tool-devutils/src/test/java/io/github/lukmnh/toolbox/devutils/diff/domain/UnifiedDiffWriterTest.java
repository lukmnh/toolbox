package io.github.lukmnh.toolbox.devutils.diff.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class UnifiedDiffWriterTest {

    private static String numbered(int from, int to) {
        return IntStream.rangeClosed(from, to).mapToObj(Integer::toString).collect(Collectors.joining("\n")) + "\n";
    }

    private static List<String> write(String oldText, String newText, List<DiffChunk> chunks) {
        return UnifiedDiffWriter.write(
                        "f.txt",
                        TextLines.of(oldText),
                        TextLines.of(newText),
                        chunks,
                        UnifiedDiffWriter.DEFAULT_CONTEXT)
                .lines();
    }

    @Test
    void identicalTextsProduceEmptyDiff() {
        assertTrue(UnifiedDiffWriter.write("f", TextLines.of("a\n"), TextLines.of("a\n"), List.of(), 3)
                .isEmpty());
    }

    @Test
    void changeInTheMiddleHasThreeLinesOfContext() {
        String newText = numbered(1, 9).replace("\n5\n", "\nlima\n");
        List<String> lines = write(numbered(1, 9), newText, List.of(new DiffChunk(ChunkType.CHANGE, 4, 5, 4, 5)));
        assertEquals(
                List.of(
                        "--- a/f.txt",
                        "+++ b/f.txt",
                        "@@ -2,7 +2,7 @@",
                        " 2",
                        " 3",
                        " 4",
                        "-5",
                        "+lima",
                        " 6",
                        " 7",
                        " 8"),
                lines);
    }

    @Test
    void insertIntoEmptyFileUsesZeroRange() {
        List<String> lines = write("", "a\nb\n", List.of(new DiffChunk(ChunkType.INSERT, 0, 0, 0, 2)));
        assertEquals(List.of("--- a/f.txt", "+++ b/f.txt", "@@ -0,0 +1,2 @@", "+a", "+b"), lines);
    }

    @Test
    void missingFinalNewlineIsMarked() {
        List<String> lines = write("a\nb", "a\nb\n", List.of(new DiffChunk(ChunkType.CHANGE, 1, 2, 1, 2)));
        assertEquals(
                List.of(
                        "--- a/f.txt",
                        "+++ b/f.txt",
                        "@@ -1,2 +1,2 @@",
                        " a",
                        "-b",
                        UnifiedDiffWriter.NO_NEWLINE,
                        "+b"),
                lines);
    }

    @Test
    void distantChangesGetSeparateHunksAndCloseOnesAreMerged() {
        String oldText = numbered(1, 30);
        List<DiffChunk> far =
                List.of(new DiffChunk(ChunkType.DELETE, 1, 2, 1, 1), new DiffChunk(ChunkType.DELETE, 20, 21, 19, 19));
        assertEquals(
                2,
                write(oldText, oldText, far).stream()
                        .filter(line -> line.startsWith("@@"))
                        .count());
        List<DiffChunk> near =
                List.of(new DiffChunk(ChunkType.DELETE, 1, 2, 1, 1), new DiffChunk(ChunkType.DELETE, 7, 8, 6, 6));
        assertEquals(
                1,
                write(oldText, oldText, near).stream()
                        .filter(line -> line.startsWith("@@"))
                        .count());
    }
}
