package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.common.ui.NotificationAction;
import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetEntry;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetNames;
import io.github.lukmnh.toolbox.productivity.budget.domain.Category;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseItem;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseSheet;
import io.github.lukmnh.toolbox.productivity.budget.domain.Income;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Aksi edit isi budget (daftar, kategori, item, pemasukan). Semua perubahan lewat {@link BudgetViewModel#edit}
 * sehingga otomatis ter-autosave. Dipanggil dari FX thread.
 */
final class BudgetEdits {

    private final BudgetViewModel viewModel;

    BudgetEdits(BudgetViewModel viewModel) {
        this.viewModel = viewModel;
    }

    // --- Daftar pengeluaran ---------------------------------------------------------------------------------

    /** @return id daftar baru */
    Optional<String> addSheet(String name) {
        Budget budget = viewModel.budgetProperty().get();
        if (budget == null) {
            return Optional.empty();
        }
        ExpenseSheet sheet = ExpenseSheet.create(BudgetNames.unique(name, sheetNames(budget, "")));
        viewModel.edit(current -> current.addSheet(sheet));
        return Optional.of(sheet.id());
    }

    void renameSheet(String sheetId, String name) {
        viewModel.edit(budget -> {
            String unique = BudgetNames.unique(name, sheetNames(budget, sheetId));
            return budget.updateSheet(sheetId, sheet -> sheet.withName(unique));
        });
    }

    void deleteSheet(String sheetId) {
        viewModel.edit(budget -> budget.removeSheet(sheetId));
    }

    void moveSheet(String sheetId, int delta) {
        viewModel.edit(budget -> budget.moveSheet(sheetId, delta));
    }

    /** Nama default berikutnya: Daftar 1, Daftar 2, … */
    String nextSheetName() {
        Budget budget = viewModel.budgetProperty().get();
        int count = budget == null ? 0 : budget.sheets().size();
        return Messages.format("productivity.budget.sheet.defaultName", count + 1);
    }

    // --- Kategori -------------------------------------------------------------------------------------------

    void addCategory(String sheetId, String name, String color) {
        editSheet(sheetId, sheet -> sheet.addCategory(Category.create(uniqueCategoryName(sheet, name, ""), color)));
    }

    void updateCategory(String sheetId, String categoryId, String name, String color) {
        editSheet(
                sheetId,
                sheet -> sheet.updateCategory(
                        categoryId,
                        category -> category.withName(uniqueCategoryName(sheet, name, categoryId))
                                .withColor(color)));
    }

    void deleteCategory(String sheetId, String categoryId) {
        editSheet(sheetId, sheet -> sheet.removeCategory(categoryId));
    }

    // --- Item -----------------------------------------------------------------------------------------------

    /** @return id item baru, kosong bila batas jumlah item tercapai */
    Optional<String> addItem(String sheetId) {
        if (!hasRoomForItem()) {
            return Optional.empty();
        }
        ExpenseItem item = ExpenseItem.blank(ExpenseItem.NO_CATEGORY);
        editSheet(sheetId, sheet -> sheet.addItem(item));
        return Optional.of(item.id());
    }

    void duplicateItem(String sheetId, String itemId) {
        if (hasRoomForItem()) {
            editSheet(sheetId, sheet -> sheet.duplicateItem(itemId));
        }
    }

    void updateItem(String sheetId, String itemId, UnaryOperator<ExpenseItem> change) {
        editSheet(sheetId, sheet -> sheet.updateItem(itemId, change));
    }

    void moveItem(String sheetId, String itemId, int delta) {
        editSheet(sheetId, sheet -> sheet.moveItem(itemId, delta));
    }

    /** Hapus lalu tampilkan toast "Batalkan" yang mengembalikan item ke posisi semula. */
    void deleteItems(String sheetId, Collection<String> itemIds) {
        Budget budget = viewModel.budgetProperty().get();
        BudgetEntry entry = viewModel.currentEntryProperty().get();
        Optional<ExpenseSheet> sheet = budget == null ? Optional.empty() : budget.sheet(sheetId);
        if (sheet.isEmpty() || entry == null || itemIds.isEmpty()) {
            return;
        }
        List<Removed<ExpenseItem>> removed = sheet.get().items().stream()
                .filter(item -> itemIds.contains(item.id()))
                .map(item -> new Removed<>(sheet.get().indexOf(item.id()), item))
                .toList();
        editSheet(sheetId, current -> current.removeItems(Set.copyOf(itemIds)));
        undoToast(
                Messages.format("productivity.budget.item.deleted", removed.size()),
                () -> restoreItems(entry.id(), sheetId, removed));
    }

    // --- Pemasukan ------------------------------------------------------------------------------------------

    /** @return id pemasukan baru */
    Optional<String> addIncome() {
        if (viewModel.budgetProperty().get() == null) {
            return Optional.empty();
        }
        Income income = Income.blank();
        viewModel.edit(budget -> budget.addIncome(income));
        return Optional.of(income.id());
    }

    void updateIncome(String incomeId, UnaryOperator<Income> change) {
        viewModel.edit(budget -> budget.updateIncome(incomeId, change));
    }

    void deleteIncome(String incomeId) {
        Budget budget = viewModel.budgetProperty().get();
        BudgetEntry entry = viewModel.currentEntryProperty().get();
        if (budget == null || entry == null) {
            return;
        }
        int index = budget.incomeIndex(incomeId);
        if (index < 0) {
            return;
        }
        Income income = budget.incomes().get(index);
        viewModel.edit(current -> current.removeIncome(incomeId));
        undoToast(
                Messages.get("productivity.budget.income.deleted"),
                () -> whenStillOpen(entry.id(), () -> viewModel.edit(current -> current.insertIncome(index, income))));
    }

    // --- Internal -------------------------------------------------------------------------------------------

    private void editSheet(String sheetId, UnaryOperator<ExpenseSheet> change) {
        viewModel.edit(budget -> budget.updateSheet(sheetId, change));
    }

    private boolean hasRoomForItem() {
        Budget budget = viewModel.budgetProperty().get();
        if (budget == null) {
            return false;
        }
        if (budget.itemCount() >= SecurityLimits.BUDGET_MAX_ITEMS) {
            viewModel
                    .context()
                    .notifier()
                    .warning(Messages.format("productivity.budget.item.limit", SecurityLimits.BUDGET_MAX_ITEMS));
            return false;
        }
        return true;
    }

    /** Disisipkan dari index terkecil supaya posisi asal setiap item kembali tepat. */
    private void restoreItems(String entryId, String sheetId, List<Removed<ExpenseItem>> removed) {
        whenStillOpen(
                entryId,
                () -> editSheet(sheetId, sheet -> {
                    ExpenseSheet restored = sheet;
                    List<Removed<ExpenseItem>> ascending = removed.stream()
                            .sorted(Comparator.comparingInt(Removed::index))
                            .toList();
                    for (Removed<ExpenseItem> entry : ascending) {
                        restored = restored.insertItem(entry.index(), entry.value());
                    }
                    return restored;
                }));
    }

    /** Undo diabaikan bila user sudah pindah ke budget lain. */
    private void whenStillOpen(String entryId, Runnable action) {
        BudgetEntry current = viewModel.currentEntryProperty().get();
        if (current != null && current.id().equals(entryId)) {
            action.run();
        }
    }

    private void undoToast(String message, Runnable undo) {
        viewModel
                .context()
                .notifier()
                .success(message, List.of(new NotificationAction(Messages.get("productivity.budget.undo"), undo)));
    }

    private static List<String> sheetNames(Budget budget, String exceptSheetId) {
        return budget.sheets().stream()
                .filter(sheet -> !sheet.id().equals(exceptSheetId))
                .map(ExpenseSheet::name)
                .toList();
    }

    private static String uniqueCategoryName(ExpenseSheet sheet, String name, String exceptCategoryId) {
        return BudgetNames.unique(
                name,
                sheet.categories().stream()
                        .filter(category -> !category.id().equals(exceptCategoryId))
                        .map(Category::name)
                        .toList());
    }

    private record Removed<T>(int index, T value) {}
}
