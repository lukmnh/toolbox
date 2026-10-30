package io.github.lukmnh.toolbox.productivity.budget.application;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetEntry;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetImporter;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetNames;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetRepository;
import io.github.lukmnh.toolbox.productivity.budget.domain.OpenedBudget;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Import file export Toolbox (.xlsx/.pdf) sebagai budget baru; tidak pernah menimpa budget yang ada (nama dibuat
 * unik).
 */
public final class ImportBudgetUseCase {

    private final BudgetImporter importer;
    private final BudgetRepository repository;
    private final Clock clock;

    public ImportBudgetUseCase(BudgetImporter importer, BudgetRepository repository, Clock clock) {
        this.importer = importer;
        this.repository = repository;
        this.clock = clock;
    }

    public OpenedBudget execute(Path source) {
        Budget imported = importer.read(source);
        String name = BudgetNames.unique(
                imported.name(),
                repository.list().stream().map(BudgetEntry::name).toList());
        Budget budget = imported.withName(name).withModified(clock.instant());
        return new OpenedBudget(repository.create(budget), budget);
    }
}
