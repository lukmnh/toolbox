package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pipeline langkah yang sudah divalidasi terhadap kolom input. Skema kolom hasil ditentukan sebelum data dibaca,
 * sehingga file bisa diproses streaming dan setiap baris hasil punya lebar yang sama.
 */
public final class ColumnPipeline {

    private final List<String> outputColumns;
    private final List<ColumnOperation> operations;

    private ColumnPipeline(List<String> outputColumns, List<ColumnOperation> operations) {
        this.outputColumns = outputColumns;
        this.operations = operations;
    }

    /**
     * Resolve langkah aktif secara berurutan.
     *
     * @throws InvalidStepException bila ada langkah yang tidak valid terhadap kolom yang tersedia
     */
    public static ColumnPipeline compile(List<String> inputColumns, List<PipelineStep> steps) {
        List<String> columns = List.copyOf(inputColumns);
        List<ColumnOperation> operations = new ArrayList<>();
        for (int index = 0; index < steps.size(); index++) {
            PipelineStep step = steps.get(index);
            if (!step.enabled()) {
                continue;
            }
            ColumnOperation operation = operation(index + 1, columns, step);
            requireUnique(index + 1, operation.outputColumns());
            columns = operation.outputColumns();
            operations.add(operation);
        }
        return new ColumnPipeline(columns, List.copyOf(operations));
    }

    /**
     * Kolom yang tersedia untuk langkah ke-{@code stepIndex} (mulai 0), untuk editor langkah. Langkah sebelumnya yang
     * tidak valid dilewati supaya editor tetap bisa dipakai selama user memperbaikinya.
     */
    public static List<String> columnsBefore(List<String> inputColumns, List<PipelineStep> steps, int stepIndex) {
        List<String> columns = List.copyOf(inputColumns);
        int end = Math.min(stepIndex, steps.size());
        for (int index = 0; index < end; index++) {
            PipelineStep step = steps.get(index);
            if (step.enabled()) {
                columns = columnsAfter(index + 1, columns, step);
            }
        }
        return columns;
    }

    public List<String> outputColumns() {
        return outputColumns;
    }

    public List<String> apply(List<String> row, TimedMatcher matcher) {
        List<String> current = row;
        for (ColumnOperation operation : operations) {
            current = operation.apply(current, matcher);
        }
        return current;
    }

    private static List<String> columnsAfter(int stepNumber, List<String> columns, PipelineStep step) {
        try {
            ColumnOperation operation = operation(stepNumber, columns, step);
            requireUnique(stepNumber, operation.outputColumns());
            return operation.outputColumns();
        } catch (InvalidStepException e) {
            // Langkah invalid diperlakukan seperti nonaktif; error-nya tetap tampil lewat pratinjau
            return columns;
        }
    }

    private static ColumnOperation operation(int stepNumber, List<String> columns, PipelineStep step) {
        return switch (step) {
            case SplitStep split -> SplitOperation.compile(stepNumber, columns, split);
            case MergeStep merge -> MergeOperation.compile(stepNumber, columns, merge);
            case ArrangeStep arrange -> ArrangeOperation.compile(stepNumber, columns, arrange);
        };
    }

    private static void requireUnique(int stepNumber, List<String> columns) {
        Set<String> seen = new HashSet<>();
        for (String column : columns) {
            if (!seen.add(column)) {
                throw new InvalidStepException(stepNumber, StepProblem.DUPLICATE_COLUMN, column);
            }
        }
    }

    /** Indeks kolom bernama {@code name}, atau error yang menyebut langkah dan nama kolomnya. */
    static int indexOf(int stepNumber, List<String> columns, String name) {
        if (name.isBlank()) {
            throw new InvalidStepException(stepNumber, StepProblem.NO_SOURCE_COLUMN, "");
        }
        int index = columns.indexOf(name);
        if (index < 0) {
            throw new InvalidStepException(stepNumber, StepProblem.MISSING_COLUMN, name);
        }
        return index;
    }
}
