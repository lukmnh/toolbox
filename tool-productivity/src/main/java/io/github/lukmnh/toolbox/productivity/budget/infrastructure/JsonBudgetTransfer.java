package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetStorageException;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetTransfer;
import java.io.IOException;
import java.nio.file.Path;

/** Export/import JSON memakai skema yang sama dengan file autosave. */
public final class JsonBudgetTransfer implements BudgetTransfer {

    private final BudgetJsonCodec codec;

    public JsonBudgetTransfer(BudgetJsonCodec codec) {
        this.codec = codec;
    }

    @Override
    public void exportJson(Budget budget, Path target) {
        try {
            codec.write(budget, target);
        } catch (IOException e) {
            throw new BudgetStorageException(target.getFileName().toString(), e);
        }
    }

    @Override
    public Budget importJson(Path source) {
        return codec.read(source);
    }
}
