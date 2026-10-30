package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.Income;
import io.github.lukmnh.toolbox.productivity.budget.domain.MonthlyCost;
import io.github.lukmnh.toolbox.productivity.budget.domain.WorkbookLabel;
import io.github.lukmnh.toolbox.productivity.budget.domain.WorkbookLabels;
import java.util.List;
import org.dhatim.fastexcel.ConditionalFormattingExpressionRule;
import org.dhatim.fastexcel.Worksheet;

/**
 * Sheet Ringkasan: tabel pemasukan ({@code SUM}), tabel per daftar yang mereferensikan baris total sheet daftar,
 * lalu sisa, rasio, dan pengeluaran per tahun. Semua angka turunan berupa formula.
 */
final class SummarySheetWriter {

    /** Sheet daftar yang sudah ditulis: nama final + baris totalnya. */
    record SheetTotalRef(String displayName, String sheetName, int totalRow) {}

    private static final int COL_A = 0;
    private static final int COL_B = 1;
    private static final int COL_C = 2;
    private static final int TITLE_ROW = 0;
    private static final int INCOME_SECTION_ROW = 2;
    private static final int SECTION_GAP = 2;
    private static final int MONEY_DISPLAY_LENGTH = 16;

    private final Worksheet sheet;
    private final WorkbookLabels labels;
    private final ColumnWidths widths = new ColumnWidths();

    private SummarySheetWriter(Worksheet sheet, WorkbookLabels labels) {
        this.sheet = sheet;
        this.labels = labels;
    }

    static void write(Worksheet sheet, Budget budget, List<SheetTotalRef> totals, WorkbookLabels labels) {
        new SummarySheetWriter(sheet, labels).write(budget, totals);
    }

    private void write(Budget budget, List<SheetTotalRef> totals) {
        sheet.value(TITLE_ROW, COL_A, budget.name());
        sheet.style(TITLE_ROW, COL_A).bold().fontSize(XlsxStyle.TITLE_FONT_SIZE).set();
        int incomeTotalRow = writeIncomes(budget.incomes());
        int expenseTotalRow = writeExpenses(incomeTotalRow + SECTION_GAP, totals);
        writeMetrics(expenseTotalRow + SECTION_GAP, incomeTotalRow, expenseTotalRow);
        widths.trackNumber(COL_B, MONEY_DISPLAY_LENGTH);
        widths.trackNumber(COL_C, MONEY_DISPLAY_LENGTH);
        widths.apply(sheet);
    }

    /** @return baris total pemasukan */
    private int writeIncomes(List<Income> incomes) {
        section(INCOME_SECTION_ROW, WorkbookLabel.INCOME_SECTION);
        int headerRow = INCOME_SECTION_ROW + 1;
        header(headerRow, WorkbookLabel.INCOME_SOURCE, WorkbookLabel.INCOME_AMOUNT, WorkbookLabel.INCOME_NOTE);
        int firstRow = headerRow + 1;
        for (int index = 0; index < incomes.size(); index++) {
            Income income = incomes.get(index);
            int row = firstRow + index;
            text(row, COL_A, income.name());
            sheet.value(row, COL_B, income.amount());
            text(row, COL_C, income.note());
            XlsxStyle.BORDERED.apply(sheet, row, COL_A);
            XlsxStyle.BORDERED.withFormat(XlsxStyle.RUPIAH_CENTS).apply(sheet, row, COL_B);
            XlsxStyle.BORDERED.apply(sheet, row, COL_C);
        }
        int totalRow = firstRow + incomes.size();
        totalLabel(totalRow, WorkbookLabel.INCOME_TOTAL);
        sum(totalRow, COL_B, firstRow, incomes.size(), XlsxStyle.RUPIAH_CENTS);
        XlsxStyle.TOTAL.apply(sheet, totalRow, COL_C);
        return totalRow;
    }

    /** @return baris total pengeluaran */
    private int writeExpenses(int sectionRow, List<SheetTotalRef> totals) {
        section(sectionRow, WorkbookLabel.EXPENSE_SECTION);
        int headerRow = sectionRow + 1;
        header(headerRow, WorkbookLabel.EXPENSE_SHEET, WorkbookLabel.EXPENSE_PRICE, WorkbookLabel.EXPENSE_MONTHLY);
        int firstRow = headerRow + 1;
        for (int index = 0; index < totals.size(); index++) {
            SheetTotalRef total = totals.get(index);
            int row = firstRow + index;
            text(row, COL_A, total.displayName());
            XlsxStyle.BORDERED.apply(sheet, row, COL_A);
            reference(row, COL_B, total, ExpenseSheetWriter.COL_PRICE);
            reference(row, COL_C, total, ExpenseSheetWriter.COL_MONTHLY);
        }
        int totalRow = firstRow + totals.size();
        totalLabel(totalRow, WorkbookLabel.EXPENSE_TOTAL);
        sum(totalRow, COL_B, firstRow, totals.size(), XlsxStyle.RUPIAH);
        sum(totalRow, COL_C, firstRow, totals.size(), XlsxStyle.RUPIAH);
        return totalRow;
    }

    /** {@code ='Skincare'!H30}: nilai ikut berubah bila sheet daftar diedit di Excel. */
    private void reference(int row, int column, SheetTotalRef total, int sourceColumn) {
        String cell = XlsxCells.ref(total.totalRow(), sourceColumn);
        XlsxCells.formula(sheet, row, column, XlsxCells.sheetRef(total.sheetName(), cell));
        XlsxStyle.BORDERED.withFormat(XlsxStyle.RUPIAH).apply(sheet, row, column);
    }

    private void writeMetrics(int firstRow, int incomeTotalRow, int expenseTotalRow) {
        int incomeRow = firstRow;
        int expenseRow = firstRow + 1;
        int remainingRow = firstRow + 2;
        int ratioRow = firstRow + 3;
        int perYearRow = firstRow + 4;
        String income = XlsxCells.ref(incomeRow, COL_B);
        String expense = XlsxCells.ref(expenseRow, COL_B);
        metric(incomeRow, WorkbookLabel.METRIC_INCOME, XlsxCells.ref(incomeTotalRow, COL_B), XlsxStyle.RUPIAH_CENTS);
        metric(expenseRow, WorkbookLabel.METRIC_EXPENSE, XlsxCells.ref(expenseTotalRow, COL_C), XlsxStyle.RUPIAH);
        metric(remainingRow, WorkbookLabel.METRIC_REMAINING, income + "-" + expense, XlsxStyle.RUPIAH_CENTS);
        metric(
                ratioRow,
                WorkbookLabel.METRIC_RATIO,
                "IF(" + income + ">0," + expense + "/" + income + ",0)",
                XlsxStyle.PERCENT);
        metric(
                perYearRow,
                WorkbookLabel.METRIC_PER_YEAR,
                expense + "*" + MonthlyCost.MONTHS_PER_YEAR.toPlainString(),
                XlsxStyle.RUPIAH);
        highlightNegative(remainingRow);
    }

    /** Conditional formatting (bukan warna statis) supaya merah/normal ikut berubah bila angka diedit di Excel. */
    private void highlightNegative(int row) {
        String expression = XlsxCells.xmlEscape("0>" + XlsxCells.ref(row, COL_B));
        sheet.range(row, COL_B, row, COL_B)
                .style()
                .fontColor(XlsxStyle.INACTIVE_FONT)
                .fillColor(XlsxStyle.INACTIVE_FILL)
                .set(new ConditionalFormattingExpressionRule(expression, true));
    }

    private void metric(int row, WorkbookLabel label, String expression, String format) {
        String text = labels.get(label);
        sheet.value(row, COL_A, text);
        widths.track(COL_A, text);
        XlsxStyle.BORDERED.withBold().apply(sheet, row, COL_A);
        XlsxCells.formula(sheet, row, COL_B, expression);
        XlsxStyle.BORDERED.withBold().withFormat(format).apply(sheet, row, COL_B);
    }

    private void section(int row, WorkbookLabel label) {
        sheet.value(row, COL_A, labels.get(label));
        XlsxStyle.BOLD.apply(sheet, row, COL_A);
    }

    private void header(int row, WorkbookLabel... columns) {
        for (int column = 0; column < columns.length; column++) {
            String text = labels.get(columns[column]);
            sheet.value(row, column, text);
            widths.track(column, text);
            XlsxStyle.HEADER.apply(sheet, row, column);
        }
    }

    private void totalLabel(int row, WorkbookLabel label) {
        String text = labels.get(label);
        sheet.value(row, COL_A, text);
        widths.track(COL_A, text);
        XlsxStyle.TOTAL.apply(sheet, row, COL_A);
    }

    private void sum(int row, int column, int firstRow, int count, String format) {
        if (count == 0) {
            sheet.value(row, column, 0);
        } else {
            String values = XlsxCells.range(firstRow, firstRow + count - 1, column);
            XlsxCells.formula(sheet, row, column, "SUM(" + values + ")");
        }
        XlsxStyle.TOTAL.withFormat(format).apply(sheet, row, column);
    }

    /** Teks user selalu sel string (Security Requirements #18). */
    private void text(int row, int column, String value) {
        sheet.value(row, column, value);
        widths.track(column, value);
    }
}
