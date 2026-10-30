package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Menulis unified diff standar (format GNU diff / {@code git diff}) yang bisa dipakai {@code git apply} dan
 * {@code patch}. Ditulis sendiri karena writer library tidak menangani {@code \ No newline at end of file}.
 */
public final class UnifiedDiffWriter {

    public static final int DEFAULT_CONTEXT = 3;
    static final String NO_NEWLINE = "\\ No newline at end of file";

    private UnifiedDiffWriter() {}

    /** @param chunks potongan berbeda dari {@link SequenceDiffer}, terurut */
    public static UnifiedDiff write(
            String fileName, TextLines oldText, TextLines newText, List<DiffChunk> chunks, int context) {
        if (chunks.isEmpty()) {
            return new UnifiedDiff(List.of());
        }
        List<String> lines = new ArrayList<>();
        lines.add("--- a/" + fileName);
        lines.add("+++ b/" + fileName);
        for (List<DiffChunk> hunk : groupHunks(chunks, context)) {
            writeHunk(lines, oldText, newText, hunk, context);
        }
        return new UnifiedDiff(lines);
    }

    /** Potongan yang jaraknya ≤ 2× context digabung ke satu hunk, supaya context tidak tumpang tindih. */
    private static List<List<DiffChunk>> groupHunks(List<DiffChunk> chunks, int context) {
        List<List<DiffChunk>> hunks = new ArrayList<>();
        List<DiffChunk> current = new ArrayList<>();
        for (DiffChunk chunk : chunks) {
            if (!current.isEmpty() && chunk.oldStart() - current.getLast().oldEnd() > 2 * context) {
                hunks.add(current);
                current = new ArrayList<>();
            }
            current.add(chunk);
        }
        hunks.add(current);
        return hunks;
    }

    private static void writeHunk(
            List<String> lines, TextLines oldText, TextLines newText, List<DiffChunk> hunk, int context) {
        DiffChunk first = hunk.getFirst();
        DiffChunk last = hunk.getLast();
        // Di luar potongan, kedua sisi identik, jadi panjang context kiri = kanan
        int oldStart = Math.max(0, first.oldStart() - context);
        int newStart = first.newStart() - (first.oldStart() - oldStart);
        int oldEnd = Math.min(oldText.size(), last.oldEnd() + context);
        int newEnd = last.newEnd() + (oldEnd - last.oldEnd());
        lines.add("@@ -" + range(oldStart, oldEnd - oldStart) + " +" + range(newStart, newEnd - newStart) + " @@");
        int cursor = oldStart;
        for (DiffChunk chunk : hunk) {
            writeContext(lines, oldText, cursor, chunk.oldStart());
            writeSide(lines, oldText, '-', chunk.oldStart(), chunk.oldEnd());
            writeSide(lines, newText, '+', chunk.newStart(), chunk.newEnd());
            cursor = chunk.oldEnd();
        }
        writeContext(lines, oldText, cursor, oldEnd);
    }

    /** Range kosong ditulis {@code <baris sebelumnya>,0} sesuai konvensi GNU diff. */
    private static String range(int start, int count) {
        return count == 0 ? start + ",0" : (start + 1) + "," + count;
    }

    private static void writeContext(List<String> lines, TextLines oldText, int from, int to) {
        writeSide(lines, oldText, ' ', from, to);
    }

    private static void writeSide(List<String> lines, TextLines text, char prefix, int from, int to) {
        for (int index = from; index < to; index++) {
            lines.add(prefix + text.line(index));
            if (text.isLast(index) && !text.endsWithNewline()) {
                lines.add(NO_NEWLINE);
            }
        }
    }
}
