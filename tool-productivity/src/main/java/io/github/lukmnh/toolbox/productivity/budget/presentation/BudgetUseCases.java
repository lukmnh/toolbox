package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.productivity.budget.application.CreateBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.DeleteBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.DuplicateBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.ExportBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.ImportBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.ListBudgetsUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.LoadBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.RenameBudgetUseCase;
import io.github.lukmnh.toolbox.productivity.budget.application.SaveBudgetUseCase;
import java.util.Objects;

/** Kumpulan use case Budget, supaya constructor tool dan ViewModel tidak terlalu panjang. */
public record BudgetUseCases(
        ListBudgetsUseCase list,
        LoadBudgetUseCase load,
        CreateBudgetUseCase create,
        SaveBudgetUseCase save,
        RenameBudgetUseCase rename,
        DuplicateBudgetUseCase duplicate,
        DeleteBudgetUseCase delete,
        ImportBudgetUseCase importFile,
        ExportBudgetUseCase exportWorkbook,
        ExportBudgetUseCase exportPdf) {

    public BudgetUseCases {
        Objects.requireNonNull(list, "list");
        Objects.requireNonNull(load, "load");
        Objects.requireNonNull(create, "create");
        Objects.requireNonNull(save, "save");
        Objects.requireNonNull(rename, "rename");
        Objects.requireNonNull(duplicate, "duplicate");
        Objects.requireNonNull(delete, "delete");
        Objects.requireNonNull(importFile, "importFile");
        Objects.requireNonNull(exportWorkbook, "exportWorkbook");
        Objects.requireNonNull(exportPdf, "exportPdf");
    }
}
