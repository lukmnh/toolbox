package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Atur kolom yang sudah di-resolve: tiap kolom hasil diambil dari satu indeks input. */
final class ArrangeOperation implements ColumnOperation {

    private final int[] sources;
    private final List<String> outputColumns;

    private ArrangeOperation(int[] sources, List<String> outputColumns) {
        this.sources = sources;
        this.outputColumns = outputColumns;
    }

    static ArrangeOperation compile(int stepNumber, List<String> columns, ArrangeStep step) {
        Set<String> unruled = new LinkedHashSet<>(columns);
        Set<String> ruled = new HashSet<>();
        List<Integer> sources = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (ColumnRule rule : step.rules()) {
            int index = columns.indexOf(rule.source());
            // Aturan untuk kolom yang sudah tidak ada (langkah sebelumnya berubah) diabaikan; duplikat juga
            if (index < 0 || !ruled.add(rule.source())) {
                continue;
            }
            unruled.remove(rule.source());
            if (rule.keep()) {
                sources.add(index);
                names.add(rule.effectiveName());
            }
        }
        for (String column : unruled) {
            sources.add(columns.indexOf(column));
            names.add(column);
        }
        if (sources.isEmpty()) {
            throw new InvalidStepException(stepNumber, StepProblem.NO_COLUMNS_LEFT, "");
        }
        return new ArrangeOperation(sources.stream().mapToInt(Integer::intValue).toArray(), List.copyOf(names));
    }

    @Override
    public List<String> outputColumns() {
        return outputColumns;
    }

    @Override
    public List<String> apply(List<String> row, TimedMatcher matcher) {
        List<String> output = new ArrayList<>(sources.length);
        for (int source : sources) {
            output.add(row.get(source));
        }
        return output;
    }
}
