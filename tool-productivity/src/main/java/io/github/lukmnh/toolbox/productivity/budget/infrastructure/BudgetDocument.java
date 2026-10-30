package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import io.github.lukmnh.toolbox.productivity.budget.domain.DurationUnit;
import io.github.lukmnh.toolbox.productivity.budget.domain.ItemStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * Skema file JSON budget (versi {@link #CURRENT_VERSION}). DTO terpisah dari domain supaya domain bebas Jackson dan
 * format file bisa berevolusi tanpa mengubah model.
 */
record BudgetDocument(
        Integer version,
        String name,
        String created,
        String modified,
        List<IncomeDocument> incomes,
        List<SheetDocument> sheets) {

    static final int CURRENT_VERSION = 1;

    record IncomeDocument(String id, String name, BigDecimal amount, String note) {}

    record SheetDocument(String id, String name, List<CategoryDocument> categories, List<ItemDocument> items) {}

    record CategoryDocument(String id, String name, String color) {}

    record ItemDocument(
            String id,
            String name,
            String brand,
            String netto,
            String categoryId,
            BigDecimal price,
            BigDecimal duration,
            DurationUnit unit,
            ItemStatus status,
            String note) {}
}
