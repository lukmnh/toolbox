package io.github.lukmnh.toolbox.productivity.budget.application;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetTransfer;
import java.nio.file.Path;

/** Tulis budget ke file JSON (biasanya temp file sebelum di-download). */
public final class ExportBudgetJsonUseCase {

    private final BudgetTransfer transfer;

    public ExportBudgetJsonUseCase(BudgetTransfer transfer) {
        this.transfer = transfer;
    }

    public void execute(Budget budget, Path target) {
        transfer.exportJson(budget, target);
    }
}
