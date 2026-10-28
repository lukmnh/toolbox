package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Menyusun baris side-by-side dari potongan diff. Pasangan baris di potongan {@link ChunkType#CHANGE} diberi highlight
 * per kata; sisa baris yang tidak berpasangan menjadi baris hapus/tambah.
 */
public final class DiffRowsBuilder {

    /** Baris sangat panjang (misalnya JSON minified) tidak di-diff per kata supaya tetap cepat. */
    static final int WORD_DIFF_MAX_CHARS = 5_000;

    private final SequenceDiffer differ;
    private final DiffOptions options;
    private final Runnable checkpoint;

    public DiffRowsBuilder(SequenceDiffer differ, DiffOptions options, Runnable checkpoint) {
        this.differ = differ;
        this.options = options;
        this.checkpoint = checkpoint;
    }

    public List<DiffRow> build(TextLines oldText, TextLines newText, List<DiffChunk> chunks) {
        List<DiffRow> rows = new ArrayList<>(Math.max(oldText.size(), newText.size()));
        int oldCursor = 0;
        int newCursor = 0;
        for (DiffChunk chunk : chunks) {
            addEqualRows(rows, oldText, newText, oldCursor, chunk.oldStart(), newCursor);
            addChunkRows(rows, oldText, newText, chunk);
            oldCursor = chunk.oldEnd();
            newCursor = chunk.newEnd();
            checkpoint.run();
        }
        addEqualRows(rows, oldText, newText, oldCursor, oldText.size(), newCursor);
        return rows;
    }

    /** Teks baru dipakai di sisi kanan karena dengan opsi "abaikan spasi/huruf" isinya bisa berbeda dari kiri. */
    private static void addEqualRows(
            List<DiffRow> rows, TextLines oldText, TextLines newText, int oldFrom, int oldTo, int newFrom) {
        for (int oldIndex = oldFrom; oldIndex < oldTo; oldIndex++) {
            int newIndex = newFrom + (oldIndex - oldFrom);
            rows.add(new DiffRow(
                    RowType.EQUAL,
                    oldIndex + 1,
                    whole(oldText.line(oldIndex), false),
                    newIndex + 1,
                    whole(newText.line(newIndex), false)));
        }
    }

    private void addChunkRows(List<DiffRow> rows, TextLines oldText, TextLines newText, DiffChunk chunk) {
        int paired = Math.min(chunk.oldLength(), chunk.newLength());
        for (int offset = 0; offset < paired; offset++) {
            int oldIndex = chunk.oldStart() + offset;
            int newIndex = chunk.newStart() + offset;
            rows.add(changedRow(oldIndex, oldText.line(oldIndex), newIndex, newText.line(newIndex)));
        }
        for (int oldIndex = chunk.oldStart() + paired; oldIndex < chunk.oldEnd(); oldIndex++) {
            rows.add(new DiffRow(
                    RowType.DELETE, oldIndex + 1, whole(oldText.line(oldIndex), true), DiffRow.NO_LINE, List.of()));
        }
        for (int newIndex = chunk.newStart() + paired; newIndex < chunk.newEnd(); newIndex++) {
            rows.add(new DiffRow(
                    RowType.INSERT, DiffRow.NO_LINE, List.of(), newIndex + 1, whole(newText.line(newIndex), true)));
        }
    }

    private DiffRow changedRow(int oldIndex, String oldLine, int newIndex, String newLine) {
        String oldDisplay = display(oldLine);
        String newDisplay = display(newLine);
        if (oldDisplay.length() > WORD_DIFF_MAX_CHARS || newDisplay.length() > WORD_DIFF_MAX_CHARS) {
            return new DiffRow(RowType.CHANGE, oldIndex + 1, whole(oldLine, true), newIndex + 1, whole(newLine, true));
        }
        List<String> oldTokens = WordTokenizer.tokenize(oldDisplay);
        List<String> newTokens = WordTokenizer.tokenize(newDisplay);
        List<DiffChunk> wordChunks = differ.diff(keys(oldTokens), keys(newTokens), checkpoint);
        boolean[] oldChanged = new boolean[oldTokens.size()];
        boolean[] newChanged = new boolean[newTokens.size()];
        for (DiffChunk chunk : wordChunks) {
            Arrays.fill(oldChanged, chunk.oldStart(), chunk.oldEnd(), true);
            Arrays.fill(newChanged, chunk.newStart(), chunk.newEnd(), true);
        }
        return new DiffRow(
                RowType.CHANGE, oldIndex + 1, merge(oldTokens, oldChanged), newIndex + 1, merge(newTokens, newChanged));
    }

    private List<String> keys(List<String> tokens) {
        return tokens.stream().map(token -> LineKey.of(token, options)).toList();
    }

    /** Token berurutan dengan status sama digabung supaya jumlah node UI per baris tetap sedikit. */
    private static List<Segment> merge(List<String> tokens, boolean[] changed) {
        List<Segment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < tokens.size(); index++) {
            if (index > 0 && changed[index] != changed[index - 1]) {
                segments.add(new Segment(current.toString(), changed[index - 1]));
                current.setLength(0);
            }
            current.append(tokens.get(index));
        }
        if (!tokens.isEmpty()) {
            segments.add(new Segment(current.toString(), changed[tokens.size() - 1]));
        }
        return segments;
    }

    private static List<Segment> whole(String line, boolean changed) {
        return List.of(new Segment(display(line), changed));
    }

    /** {@code \r} dari file CRLF tidak ditampilkan; teks asli tetap utuh di unified diff. */
    private static String display(String line) {
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }
}
