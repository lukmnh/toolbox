package io.github.lukmnh.toolbox.productivity.budget.application;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetEntry;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetNames;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetRepository;
import io.github.lukmnh.toolbox.productivity.budget.domain.OpenedBudget;
import java.time.Clock;

/** Salin budget ke file baru bernama unik ({@code Nama (2)}). */
public final class DuplicateBudgetUseCase {

    private final BudgetRepository repository;
    private final Clock clock;

    public DuplicateBudgetUseCase(BudgetRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public OpenedBudget execute(Budget source) {
        String name = BudgetNames.unique(
                source.name(), repository.list().stream().map(BudgetEntry::name).toList());
        Budget copy = source.withName(name).withCreated(clock.instant());
        return new OpenedBudget(repository.create(copy), copy);
    }
}
