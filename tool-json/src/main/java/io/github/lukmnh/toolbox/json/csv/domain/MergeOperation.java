package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Merge yang sudah di-resolve: {@code layout} memetakan tiap kolom hasil ke indeks input atau ke nilai gabungan. */
final class MergeOperation implements ColumnOperation {

    /** Penanda di {@code layout}: posisi kolom hasil gabungan. */
    private static final int MERGED = -1;

    private final int[] sources;
    private final String separator;
    private final boolean skipEmpty;
    private final int[] layout;
    private final List<String> outputColumns;

    private MergeOperation(
            int[] sources, String separator, boolean skipEmpty, int[] layout, List<String> outputColumns) {
        this.sources = sources;
        this.separator = separator;
        this.skipEmpty = skipEmpty;
        this.layout = layout;
        this.outputColumns = outputColumns;
    }

    static MergeOperation compile(int stepNumber, List<String> columns, MergeStep step) {
        if (step.sourceColumns().size() < MergeStep.MIN_SOURCES) {
            throw new InvalidStepException(stepNumber, StepProblem.TOO_FEW_MERGE_COLUMNS, "");
        }
        String newName = step.newName().strip();
        if (newName.isEmpty()) {
            throw new InvalidStepException(stepNumber, StepProblem.EMPTY_NEW_NAME, "");
        }
        int[] sources = resolveSources(stepNumber, columns, step.sourceColumns());
        int[] layout = layout(columns.size(), sources, step.removeSources());
        List<String> output = new ArrayList<>(layout.length);
        for (int index : layout) {
            output.add(index == MERGED ? newName : columns.get(index));
        }
        return new MergeOperation(sources, step.separator(), step.skipEmpty(), layout, List.copyOf(output));
    }

    @Override
    public List<String> outputColumns() {
        return outputColumns;
    }

    @Override
    public List<String> apply(List<String> row, TimedMatcher matcher) {
        String merged = merge(row);
        List<String> output = new ArrayList<>(layout.length);
        for (int index : layout) {
            output.add(index == MERGED ? merged : row.get(index));
        }
        return output;
    }

    private String merge(List<String> row) {
        StringBuilder merged = new StringBuilder();
        boolean first = true;
        for (int source : sources) {
            String value = row.get(source);
            if (skipEmpty && value.isBlank()) {
                continue;
            }
            if (!first) {
                merged.append(separator);
            }
            merged.append(value);
            first = false;
        }
        return merged.toString();
    }

    private static int[] resolveSources(int stepNumber, List<String> columns, List<String> names) {
        Set<String> seen = new HashSet<>();
        int[] sources = new int[names.size()];
        for (int index = 0; index < names.size(); index++) {
            String name = names.get(index);
            if (!seen.add(name)) {
                throw new InvalidStepException(stepNumber, StepProblem.DUPLICATE_MERGE_COLUMN, name);
            }
            sources[index] = ColumnPipeline.indexOf(stepNumber, columns, name);
        }
        return sources;
    }

    /**
     * Kolom asal dihapus → hasil menempati posisi kolom asal paling kiri. Kolom asal dipertahankan → hasil
     * disisipkan tepat setelah kolom asal paling kanan.
     */
    private static int[] layout(int width, int[] sources, boolean removeSources) {
        Set<Integer> sourceSet = new HashSet<>();
        int leftmost = Integer.MAX_VALUE;
        int rightmost = -1;
        for (int source : sources) {
            sourceSet.add(source);
            leftmost = Math.min(leftmost, source);
            rightmost = Math.max(rightmost, source);
        }
        List<Integer> layout = new ArrayList<>(width + 1);
        for (int index = 0; index < width; index++) {
            boolean isSource = sourceSet.contains(index);
            if (!isSource || !removeSources) {
                layout.add(index);
            }
            if (removeSources ? index == leftmost : index == rightmost) {
                layout.add(MERGED);
            }
        }
        return layout.stream().mapToInt(Integer::intValue).toArray();
    }
}
