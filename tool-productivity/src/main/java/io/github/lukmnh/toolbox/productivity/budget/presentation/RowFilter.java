package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.productivity.budget.domain.Category;
import io.github.lukmnh.toolbox.productivity.budget.domain.ItemStatus;

/** Filter baris tabel item berdasarkan kategori atau status. */
sealed interface RowFilter {

    boolean test(ItemRow row);

    String label();

    /** Tanpa filter. */
    record All(String label) implements RowFilter {
        @Override
        public boolean test(ItemRow row) {
            return true;
        }
    }

    record Uncategorized() implements RowFilter {
        @Override
        public boolean test(ItemRow row) {
            return !row.item().hasCategory();
        }

        @Override
        public String label() {
            return Messages.get("productivity.budget.category.none");
        }
    }

    record InCategory(Category category) implements RowFilter {
        @Override
        public boolean test(ItemRow row) {
            return row.item().categoryId().equals(category.id());
        }

        @Override
        public String label() {
            return category.name();
        }
    }

    record WithStatus(ItemStatus status) implements RowFilter {
        @Override
        public boolean test(ItemRow row) {
            return row.item().status() == status;
        }

        @Override
        public String label() {
            return BudgetLabels.status(status);
        }
    }
}
