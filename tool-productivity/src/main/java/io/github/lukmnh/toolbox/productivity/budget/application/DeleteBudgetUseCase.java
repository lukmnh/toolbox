package io.github.lukmnh.toolbox.productivity.budget.application;

import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetRepository;

/** Hapus file budget (konfirmasi dilakukan di presentation). */
public final class DeleteBudgetUseCase {

    private final BudgetRepository repository;

    public DeleteBudgetUseCase(BudgetRepository repository) {
        this.repository = repository;
    }

    public void execute(String id) {
        repository.delete(id);
    }
}
