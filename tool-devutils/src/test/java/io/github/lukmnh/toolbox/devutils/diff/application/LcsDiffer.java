package io.github.lukmnh.toolbox.devutils.diff.application;

import io.github.lukmnh.toolbox.devutils.diff.domain.ChunkType;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffChunk;
import io.github.lukmnh.toolbox.devutils.diff.domain.SequenceDiffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Fake {@link SequenceDiffer} berbasis LCS O(n·m) sederhana, cukup untuk input kecil di test use case. */
final class LcsDiffer implements SequenceDiffer {

    @Override
    public <T> List<DiffChunk> diff(List<T> original, List<T> revised, Runnable checkpoint) {
        checkpoint.run();
        int[][] lcs = table(original, revised);
        List<DiffChunk> chunks = new ArrayList<>();
        int oldIndex = 0;
        int newIndex = 0;
        while (oldIndex < original.size() || newIndex < revised.size()) {
            if (oldIndex < original.size()
                    && newIndex < revised.size()
                    && Objects.equals(original.get(oldIndex), revised.get(newIndex))) {
                oldIndex++;
                newIndex++;
                continue;
            }
            int oldStart = oldIndex;
            int newStart = newIndex;
            while (oldIndex < original.size() || newIndex < revised.size()) {
                boolean match = oldIndex < original.size()
                        && newIndex < revised.size()
                        && Objects.equals(original.get(oldIndex), revised.get(newIndex));
                if (match) {
                    break;
                }
                if (newIndex >= revised.size()
                        || (oldIndex < original.size() && lcs[oldIndex + 1][newIndex] >= lcs[oldIndex][newIndex + 1])) {
                    oldIndex++;
                } else {
                    newIndex++;
                }
            }
            chunks.add(new DiffChunk(
                    type(oldIndex - oldStart, newIndex - newStart), oldStart, oldIndex, newStart, newIndex));
        }
        return chunks;
    }

    private static <T> int[][] table(List<T> original, List<T> revised) {
        int[][] lcs = new int[original.size() + 1][revised.size() + 1];
        for (int i = original.size() - 1; i >= 0; i--) {
            for (int j = revised.size() - 1; j >= 0; j--) {
                lcs[i][j] = Objects.equals(original.get(i), revised.get(j))
                        ? lcs[i + 1][j + 1] + 1
                        : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        return lcs;
    }

    private static ChunkType type(int oldLength, int newLength) {
        if (oldLength == 0) {
            return ChunkType.INSERT;
        }
        return newLength == 0 ? ChunkType.DELETE : ChunkType.CHANGE;
    }
}
