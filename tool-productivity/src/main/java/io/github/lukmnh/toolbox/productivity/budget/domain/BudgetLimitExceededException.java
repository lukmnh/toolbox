package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.io.Serial;

/** Jumlah item melebihi batas keamanan (Security Requirements #19). */
public final class BudgetLimitExceededException extends BudgetException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int count;
    private final int max;

    public BudgetLimitExceededException(int count, int max) {
        super("Budget item count exceeds the limit: " + count + " > " + max, null);
        this.count = count;
        this.max = max;
    }

    public int count() {
        return count;
    }

    public int max() {
        return max;
    }
}
