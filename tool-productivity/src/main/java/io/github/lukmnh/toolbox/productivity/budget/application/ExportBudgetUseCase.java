package io.github.lukmnh.toolbox.productivity.budget.application;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetExportWriter;
import io.github.lukmnh.toolbox.productivity.budget.domain.WorkbookLabels;
import java.nio.file.Path;

/** Tulis budget ke file export (format ditentukan {@link BudgetExportWriter}), biasanya temp file sebelum download. */
public final class ExportBudgetUseCase {

    private final BudgetExportWriter writer;

    public ExportBudgetUseCase(BudgetExportWriter writer) {
        this.writer = writer;
    }

    public void execute(Budget budget, WorkbookLabels labels, Path target) {
        writer.write(budget, labels, target);
    }
}
