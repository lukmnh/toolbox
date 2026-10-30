package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import io.github.lukmnh.toolbox.common.report.PdfReport;
import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetCalculator;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetExportWriter;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetStorageException;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetSummary;
import io.github.lukmnh.toolbox.productivity.budget.domain.Category;
import io.github.lukmnh.toolbox.productivity.budget.domain.CategoryTotal;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseItem;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseSheet;
import io.github.lukmnh.toolbox.productivity.budget.domain.Income;
import io.github.lukmnh.toolbox.productivity.budget.domain.SheetTotals;
import io.github.lukmnh.toolbox.productivity.budget.domain.WorkbookLabel;
import io.github.lukmnh.toolbox.productivity.budget.domain.WorkbookLabels;
import io.github.lukmnh.toolbox.productivity.shared.domain.RupiahFormat;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.stream.Stream;

/**
 * Export {@code .pdf}: ringkasan (metrik, pemasukan, total per daftar), lalu per daftar total per kategori dan tabel
 * item seperti sheet Excel. JSON budget dilampirkan ({@value #ATTACHMENT}) supaya PDF bisa di-import ulang.
 */
public final class PdfBudgetReport implements BudgetExportWriter {

    static final String ATTACHMENT = "budget.json";

    private static final int DURATION_DECIMALS = 3;
    private static final int RATIO_DECIMALS = 2;
    private static final String PERCENT = "%";
    private static final String NOT_AVAILABLE = "-";
    /** Warna kelompok tanpa kategori tidak ditampilkan di PDF; hanya syarat {@link BudgetCalculator#categoryTotals}. */
    private static final String UNUSED_COLOR = "#000000";

    private static final List<WorkbookLabel> ITEM_HEADERS = List.of(
            WorkbookLabel.HEADER_NO,
            WorkbookLabel.HEADER_NAME,
            WorkbookLabel.HEADER_BRAND,
            WorkbookLabel.HEADER_NETTO,
            WorkbookLabel.HEADER_DURATION,
            WorkbookLabel.HEADER_UNIT,
            WorkbookLabel.HEADER_PRICE,
            WorkbookLabel.HEADER_MONTHLY,
            WorkbookLabel.HEADER_CATEGORY,
            WorkbookLabel.HEADER_STATUS,
            WorkbookLabel.HEADER_NOTE);

    private final BudgetJsonCodec codec;

    public PdfBudgetReport(BudgetJsonCodec codec) {
        this.codec = codec;
    }

    @Override
    public void write(Budget budget, WorkbookLabels labels, Path target) {
        try {
            byte[] data = codec.toBytes(budget);
            report(budget, labels).attach(ATTACHMENT, data).writeTo(target);
        } catch (IOException e) {
            throw new BudgetStorageException(target.getFileName().toString(), e);
        }
    }

    private static PdfReport report(Budget budget, WorkbookLabels labels) {
        BudgetSummary summary = BudgetCalculator.summarize(budget);
        PdfReport report = PdfReport.titled(budget.name()).keyValues(metrics(summary, labels));
        report.heading(labels.get(WorkbookLabel.INCOME_SECTION))
                .table(
                        headers(
                                labels,
                                WorkbookLabel.INCOME_SOURCE,
                                WorkbookLabel.INCOME_AMOUNT,
                                WorkbookLabel.INCOME_NOTE),
                        incomeRows(budget.incomes(), summary, labels));
        report.heading(labels.get(WorkbookLabel.EXPENSE_SECTION))
                .table(
                        headers(
                                labels,
                                WorkbookLabel.EXPENSE_SHEET,
                                WorkbookLabel.EXPENSE_PRICE,
                                WorkbookLabel.EXPENSE_MONTHLY),
                        expenseRows(summary, labels));
        for (int index = 0; index < budget.sheets().size(); index++) {
            sheet(report, budget.sheets().get(index), summary.sheets().get(index), labels);
        }
        return report;
    }

    private static SequencedMap<String, String> metrics(BudgetSummary summary, WorkbookLabels labels) {
        SequencedMap<String, String> metrics = new LinkedHashMap<>();
        metrics.put(labels.get(WorkbookLabel.METRIC_INCOME), moneyCents(summary.totalIncome()));
        metrics.put(labels.get(WorkbookLabel.METRIC_EXPENSE), money(summary.totalMonthly()));
        metrics.put(labels.get(WorkbookLabel.METRIC_REMAINING), moneyCents(summary.remaining()));
        metrics.put(
                labels.get(WorkbookLabel.METRIC_RATIO),
                summary.expenseRatioPercent()
                        .map(ratio -> RupiahFormat.plain(ratio, RATIO_DECIMALS) + PERCENT)
                        .orElse(NOT_AVAILABLE));
        metrics.put(labels.get(WorkbookLabel.METRIC_PER_YEAR), money(summary.perYear()));
        return metrics;
    }

    private static List<List<String>> incomeRows(List<Income> incomes, BudgetSummary summary, WorkbookLabels labels) {
        List<List<String>> rows = new ArrayList<>();
        for (Income income : incomes) {
            rows.add(List.of(income.name(), moneyCents(income.amount()), income.note()));
        }
        rows.add(List.of(labels.get(WorkbookLabel.INCOME_TOTAL), moneyCents(summary.totalIncome()), ""));
        return rows;
    }

    private static List<List<String>> expenseRows(BudgetSummary summary, WorkbookLabels labels) {
        List<List<String>> rows = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (SheetTotals totals : summary.sheets()) {
            rows.add(List.of(totals.sheetName(), money(totals.totalPrice()), money(totals.totalMonthly())));
            totalPrice = totalPrice.add(totals.totalPrice());
        }
        rows.add(List.of(labels.get(WorkbookLabel.EXPENSE_TOTAL), money(totalPrice), money(summary.totalMonthly())));
        return rows;
    }

    private static void sheet(PdfReport report, ExpenseSheet sheet, SheetTotals totals, WorkbookLabels labels) {
        report.heading(sheet.name());
        List<CategoryTotal> categories = BudgetCalculator.categoryTotals(sheet, UNUSED_COLOR);
        if (!categories.isEmpty()) {
            List<List<String>> rows = categories.stream()
                    .map(category -> List.of(
                            category.uncategorized() ? labels.get(WorkbookLabel.CATEGORY_NONE) : category.name(),
                            money(category.monthly())))
                    .toList();
            report.paragraph(labels.get(WorkbookLabel.CATEGORY_SECTION))
                    .table(headers(labels, WorkbookLabel.HEADER_CATEGORY, WorkbookLabel.HEADER_MONTHLY), rows);
        }
        report.table(ITEM_HEADERS.stream().map(labels::get).toList(), itemRows(sheet, totals, labels));
    }

    private static List<List<String>> itemRows(ExpenseSheet sheet, SheetTotals totals, WorkbookLabels labels) {
        List<List<String>> rows = new ArrayList<>();
        List<ExpenseItem> items = sheet.items();
        for (int index = 0; index < items.size(); index++) {
            ExpenseItem item = items.get(index);
            String category =
                    sheet.category(item.categoryId()).map(Category::name).orElse("");
            rows.add(List.of(
                    Integer.toString(index + 1),
                    item.name(),
                    item.brand(),
                    item.netto(),
                    RupiahFormat.plain(item.duration(), DURATION_DECIMALS),
                    labels.get(WorkbookLabel.unit(item.unit())),
                    money(item.price()),
                    money(item.monthlyCost()),
                    category,
                    labels.get(WorkbookLabel.status(item.status())),
                    item.note()));
        }
        // Baris total: label di kolom pertama, total harga & rata-rata/bulan di kolomnya masing-masing
        List<String> total = new ArrayList<>(Collections.nCopies(ITEM_HEADERS.size(), ""));
        total.set(0, labels.get(WorkbookLabel.TOTAL_ACTIVE));
        total.set(ITEM_HEADERS.indexOf(WorkbookLabel.HEADER_PRICE), money(totals.totalPrice()));
        total.set(ITEM_HEADERS.indexOf(WorkbookLabel.HEADER_MONTHLY), money(totals.totalMonthly()));
        rows.add(total);
        return rows;
    }

    private static List<String> headers(WorkbookLabels labels, WorkbookLabel... columns) {
        return Stream.of(columns).map(labels::get).toList();
    }

    private static String money(BigDecimal amount) {
        return RupiahFormat.format(amount);
    }

    private static String moneyCents(BigDecimal amount) {
        return RupiahFormat.format(amount, RupiahFormat.TWO_DECIMALS);
    }
}
