package io.github.lukmnh.toolbox.productivity.budget.application;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetEntry;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetNames;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetRepository;
import io.github.lukmnh.toolbox.productivity.budget.domain.OpenedBudget;
import java.time.Clock;
import java.util.List;

/**
 * Ganti nama budget. {@code current} adalah state terbaru di layar, sehingga perubahan yang belum ter-autosave ikut
 * tersimpan di file baru.
 */
public final class RenameBudgetUseCase {

    private final BudgetRepository repository;
    private final Clock clock;

    public RenameBudgetUseCase(BudgetRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public OpenedBudget execute(String id, Budget current, String newName) {
        List<String> others = repository.list().stream()
                .filter(entry -> !entry.id().equals(id))
                .map(BudgetEntry::name)
                .toList();
        Budget renamed = current.withName(BudgetNames.unique(newName, others)).withModified(clock.instant());
        return new OpenedBudget(repository.rename(id, renamed), renamed);
    }
}
