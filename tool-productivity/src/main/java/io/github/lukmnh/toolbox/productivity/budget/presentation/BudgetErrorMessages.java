package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetException;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetFileTooLargeException;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetLimitExceededException;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetStorageException;
import io.github.lukmnh.toolbox.productivity.budget.domain.InvalidBudgetFileException;
import java.util.Locale;
import javafx.stage.Window;

/** Memetakan error domain Budget ke pesan berbahasa Indonesia. */
final class BudgetErrorMessages {

    private BudgetErrorMessages() {}

    static String describe(Throwable error) {
        if (!(error instanceof BudgetException budgetError)) {
            return ErrorDialogs.describe(error);
        }
        return switch (budgetError) {
            case BudgetStorageException e -> Messages.format("productivity.budget.error.storage", e.fileName());
            case InvalidBudgetFileException e ->
                Messages.format(
                        "productivity.budget.error.invalid."
                                + e.problem().name().toLowerCase(Locale.ROOT),
                        e.fileName(),
                        e.location());
            case BudgetFileTooLargeException e ->
                Messages.format(
                        "productivity.budget.error.tooLarge",
                        e.fileName(),
                        ByteSizeFormat.format(e.sizeBytes()),
                        ByteSizeFormat.format(e.maxBytes()));
            case BudgetLimitExceededException e ->
                Messages.format("productivity.budget.error.tooManyItems", e.count(), e.max());
        };
    }

    static void show(Window owner, Throwable error) {
        if (error instanceof BudgetException) {
            ErrorDialogs.show(owner, Messages.get("productivity.budget.error.header"), describe(error));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }
}
