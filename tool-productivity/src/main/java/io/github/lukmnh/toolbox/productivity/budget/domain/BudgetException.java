package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.io.Serial;

/**
 * Semua error domain Budget. Sealed supaya presentation bisa memetakan setiap jenis ke pesan user dengan
 * {@code switch} yang exhaustive.
 */
public abstract sealed class BudgetException extends RuntimeException
        permits BudgetStorageException,
                InvalidBudgetFileException,
                BudgetFileTooLargeException,
                BudgetLimitExceededException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected BudgetException(String message, Throwable cause) {
        super(message, cause);
    }
}
