package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Kalkulasi total budget. Logic murni tanpa I/O, jadi cukup class biasa. */
public final class BudgetCalculator {

    private BudgetCalculator() {}

    public static BudgetSummary summarize(Budget budget) {
        List<SheetTotals> sheets =
                budget.sheets().stream().map(BudgetCalculator::totals).toList();
        BigDecimal totalMonthly =
                sheets.stream().map(SheetTotals::totalMonthly).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BudgetSummary(totalIncome(budget), sheets, totalMonthly);
    }

    public static BigDecimal totalIncome(Budget budget) {
        return budget.incomes().stream().map(Income::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static SheetTotals totals(ExpenseSheet sheet) {
        List<ExpenseItem> active =
                sheet.items().stream().filter(item -> item.status().counts()).toList();
        BigDecimal price = active.stream().map(ExpenseItem::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        int missing = (int) active.stream().filter(ExpenseItem::missingDuration).count();
        return new SheetTotals(sheet.id(), sheet.name(), price, monthly(active, item -> true), missing);
    }

    /** Per kategori sesuai urutan legend; item tanpa kategori di akhir (hanya bila ada). */
    public static List<CategoryTotal> categoryTotals(ExpenseSheet sheet, String uncategorizedColor) {
        List<ExpenseItem> active =
                sheet.items().stream().filter(item -> item.status().counts()).toList();
        List<CategoryTotal> totals = new ArrayList<>();
        for (Category category : sheet.categories()) {
            BigDecimal sum = monthly(active, item -> item.categoryId().equals(category.id()));
            totals.add(new CategoryTotal(category.id(), category.name(), category.color(), sum));
        }
        if (active.stream().anyMatch(item -> !item.hasCategory())) {
            BigDecimal sum = monthly(active, item -> !item.hasCategory());
            totals.add(new CategoryTotal(ExpenseItem.NO_CATEGORY, "", uncategorizedColor, sum));
        }
        return List.copyOf(totals);
    }

    private static BigDecimal monthly(List<ExpenseItem> items, Predicate<ExpenseItem> filter) {
        return items.stream().filter(filter).map(ExpenseItem::monthlyCost).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
