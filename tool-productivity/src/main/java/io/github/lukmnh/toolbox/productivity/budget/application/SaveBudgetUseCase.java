package io.github.lukmnh.toolbox.productivity.budget.application;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetRepository;
import java.time.Clock;

/** Autosave: timpa file budget secara atomic dengan waktu ubah terbaru. */
public final class SaveBudgetUseCase {

    private final BudgetRepository repository;
    private final Clock clock;

    public SaveBudgetUseCase(BudgetRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void execute(String id, Budget budget) {
        repository.save(id, budget.withModified(clock.instant()));
    }
}
