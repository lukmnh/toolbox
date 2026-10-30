package io.github.lukmnh.toolbox.productivity.budget.application;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetEntry;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetNames;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetRepository;
import io.github.lukmnh.toolbox.productivity.budget.domain.OpenedBudget;
import java.time.Clock;

/** Buat budget kosong (satu daftar) dengan nama yang dibuat unik. */
public final class CreateBudgetUseCase {

    private final BudgetRepository repository;
    private final Clock clock;

    public CreateBudgetUseCase(BudgetRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public OpenedBudget execute(String desiredName, String firstSheetName) {
        String name = BudgetNames.unique(
                desiredName, repository.list().stream().map(BudgetEntry::name).toList());
        Budget budget = Budget.create(name, firstSheetName, clock.instant());
        return new OpenedBudget(repository.create(budget), budget);
    }
}
