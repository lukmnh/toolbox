package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.util.Objects;

/** Budget yang baru dibuat/dibuka beserta entry penyimpanannya. */
public record OpenedBudget(BudgetEntry entry, Budget budget) {

    public OpenedBudget {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(budget, "budget");
    }
}
