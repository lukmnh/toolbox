package io.github.lukmnh.toolbox.productivity.budget.application;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetWorkbookWriter;
import io.github.lukmnh.toolbox.productivity.budget.domain.WorkbookLabels;
import java.nio.file.Path;

/** Tulis budget ke {@code .xlsx} (sheet Ringkasan + sheet per daftar, formula Excel). */
public final class ExportBudgetWorkbookUseCase {

    private final BudgetWorkbookWriter writer;

    public ExportBudgetWorkbookUseCase(BudgetWorkbookWriter writer) {
        this.writer = writer;
    }

    public void execute(Budget budget, WorkbookLabels labels, Path target) {
        writer.write(budget, labels, target);
    }
}
