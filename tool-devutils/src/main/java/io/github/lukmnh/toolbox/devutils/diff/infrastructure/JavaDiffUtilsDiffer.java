package io.github.lukmnh.toolbox.devutils.diff.infrastructure;

import com.github.difflib.algorithm.Change;
import com.github.difflib.algorithm.myers.MyersDiffWithLinearSpace;
import com.github.difflib.patch.DeltaType;
import io.github.lukmnh.toolbox.devutils.diff.domain.ChunkType;
import io.github.lukmnh.toolbox.devutils.diff.domain.DiffChunk;
import io.github.lukmnh.toolbox.devutils.diff.domain.SequenceDiffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Adapter java-diff-utils. Memakai varian Myers linear-space supaya memori tetap kecil untuk file 100 ribu baris
 * dengan banyak perbedaan.
 */
public final class JavaDiffUtilsDiffer implements SequenceDiffer {

    /** Checkpoint pembatalan tiap N perbandingan elemen: cukup jarang supaya murah, cukup sering untuk cancel cepat. */
    private static final int CHECKPOINT_INTERVAL = 4_096;

    @Override
    public <T> List<DiffChunk> diff(List<T> original, List<T> revised, Runnable checkpoint) {
        List<Change> changes =
                new MyersDiffWithLinearSpace<T>(checkpointing(checkpoint)).computeDiff(original, revised, null);
        return mergeAdjacent(changes);
    }

    /** Algoritma tidak bisa di-interrupt, jadi pembatalan disisipkan lewat equalizer yang dipanggil terus-menerus. */
    private static <T> BiPredicate<T, T> checkpointing(Runnable checkpoint) {
        int[] calls = {0};
        return (left, right) -> {
            if (++calls[0] % CHECKPOINT_INTERVAL == 0) {
                checkpoint.run();
            }
            return Objects.equals(left, right);
        };
    }

    /**
     * Library bisa mengembalikan hapus lalu tambah sebagai dua perubahan bersebelahan; digabung menjadi satu
     * {@link ChunkType#CHANGE} supaya baris lama dan baru dipasangkan (dan highlight per kata berfungsi).
     */
    private static List<DiffChunk> mergeAdjacent(List<Change> changes) {
        List<DiffChunk> chunks = new ArrayList<>();
        for (Change change : changes) {
            if (change.deltaType == DeltaType.EQUAL) {
                continue;
            }
            DiffChunk chunk = new DiffChunk(
                    type(change.deltaType),
                    change.startOriginal,
                    change.endOriginal,
                    change.startRevised,
                    change.endRevised);
            if (!chunks.isEmpty() && touches(chunks.getLast(), chunk)) {
                chunk = combine(chunks.removeLast(), chunk);
            }
            chunks.add(chunk);
        }
        return chunks;
    }

    private static boolean touches(DiffChunk previous, DiffChunk next) {
        return previous.oldEnd() == next.oldStart() && previous.newEnd() == next.newStart();
    }

    private static DiffChunk combine(DiffChunk previous, DiffChunk next) {
        DiffChunk merged =
                new DiffChunk(ChunkType.CHANGE, previous.oldStart(), next.oldEnd(), previous.newStart(), next.newEnd());
        if (merged.oldLength() == 0) {
            return new DiffChunk(
                    ChunkType.INSERT, merged.oldStart(), merged.oldEnd(), merged.newStart(), merged.newEnd());
        }
        if (merged.newLength() == 0) {
            return new DiffChunk(
                    ChunkType.DELETE, merged.oldStart(), merged.oldEnd(), merged.newStart(), merged.newEnd());
        }
        return merged;
    }

    private static ChunkType type(DeltaType deltaType) {
        return switch (deltaType) {
            case INSERT -> ChunkType.INSERT;
            case DELETE -> ChunkType.DELETE;
            case CHANGE -> ChunkType.CHANGE;
            case EQUAL -> throw new IllegalArgumentException("EQUAL is not a change");
        };
    }
}
