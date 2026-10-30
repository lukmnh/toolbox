package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/** Semua {@link WorkbookLabel} beserta teksnya; lengkap dijamin saat dibuat. */
public record WorkbookLabels(Map<WorkbookLabel, String> texts) {

    public WorkbookLabels {
        EnumMap<WorkbookLabel, String> copy = new EnumMap<>(WorkbookLabel.class);
        copy.putAll(texts);
        for (WorkbookLabel label : WorkbookLabel.values()) {
            if (copy.get(label) == null) {
                throw new IllegalArgumentException("Workbook label not set: " + label);
            }
        }
        texts = Map.copyOf(copy);
    }

    public static WorkbookLabels from(Function<WorkbookLabel, String> text) {
        Map<WorkbookLabel, String> texts = new EnumMap<>(WorkbookLabel.class);
        for (WorkbookLabel label : WorkbookLabel.values()) {
            texts.put(label, text.apply(label));
        }
        return new WorkbookLabels(texts);
    }

    public String get(WorkbookLabel label) {
        return texts.get(label);
    }
}
