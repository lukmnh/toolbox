package io.github.lukmnh.toolbox.productivity.budget.application;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetRepository;

/** Buka satu budget dari penyimpanan. */
public final class LoadBudgetUseCase {

    private final BudgetRepository repository;

    public LoadBudgetUseCase(BudgetRepository repository) {
        this.repository = repository;
    }

    public Budget execute(String id) {
        return repository.load(id);
    }
}
