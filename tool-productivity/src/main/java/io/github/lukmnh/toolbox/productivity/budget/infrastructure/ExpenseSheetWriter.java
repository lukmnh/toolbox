package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import io.github.lukmnh.toolbox.productivity.budget.domain.Category;
import io.github.lukmnh.toolbox.productivity.budget.domain.DurationUnit;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseItem;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseSheet;
import io.github.lukmnh.toolbox.productivity.budget.domain.ItemStatus;
import io.github.lukmnh.toolbox.productivity.budget.domain.MonthlyCost;
import io.github.lukmnh.toolbox.productivity.budget.domain.WorkbookLabel;
import io.github.lukmnh.toolbox.productivity.budget.domain.WorkbookLabels;
import java.util.List;
import org.dhatim.fastexcel.Worksheet;

/**
 * Sheet satu daftar pengeluaran, meniru layout spreadsheet: judul, legend kategori, header hijau tua, item (zebra,
 * nonaktif merah, kolom No berwarna kategori), lalu baris total {@code SUMIF} item aktif.
 */
final class ExpenseSheetWriter {

    static final int COL_NO = 0;
    static final int COL_NAME = 1;
    static final int COL_BRAND = 2;
    static final int COL_NETTO = 3;
    static final int COL_DURATION = 4;
    static final int COL_UNIT = 5;
    static final int COL_PRICE = 6;
    static final int COL_MONTHLY = 7;
    static final int COL_STATUS = 8;
    static final int COL_NOTE = 9;

    private static final List<WorkbookLabel> HEADERS = List.of(
            WorkbookLabel.HEADER_NO,
            WorkbookLabel.HEADER_NAME,
            WorkbookLabel.HEADER_BRAND,
            WorkbookLabel.HEADER_NETTO,
            WorkbookLabel.HEADER_DURATION,
            WorkbookLabel.HEADER_UNIT,
            WorkbookLabel.HEADER_PRICE,
            WorkbookLabel.HEADER_MONTHLY,
            WorkbookLabel.HEADER_STATUS,
            WorkbookLabel.HEADER_NOTE);

    private static final int TITLE_ROW = 0;
    private static final int LEGEND_FIRST_ROW = 1;
    private static final int LEGEND_PER_ROW = 4;
    /** Satu baris kosong antara legend/judul dan header tabel. */
    private static final int HEADER_GAP = 2;
    /** Legend: sel warna di kolom B, nama di kolom C, lalu D/E, dst. */
    private static final int LEGEND_FIRST_COLUMN = 1;

    private static final int LEGEND_COLUMNS_PER_ENTRY = 2;
    /** Perkiraan lebar tampilan angka Rupiah jutaan ({@code Rp12.345.678}). */
    private static final int MONEY_DISPLAY_LENGTH = 14;

    private final Worksheet sheet;
    private final WorkbookLabels labels;
    private final ColumnWidths widths = new ColumnWidths();

    private ExpenseSheetWriter(Worksheet sheet, WorkbookLabels labels) {
        this.sheet = sheet;
        this.labels = labels;
    }

    /** @return index baris total (0-based), direferensikan sheet Ringkasan */
    static int write(Worksheet sheet, ExpenseSheet expenses, WorkbookLabels labels) {
        return new ExpenseSheetWriter(sheet, labels).write(expenses);
    }

    private int write(ExpenseSheet expenses) {
        sheet.value(TITLE_ROW, COL_NO, expenses.name());
        sheet.style(TITLE_ROW, COL_NO)
                .bold()
                .fontSize(XlsxStyle.TITLE_FONT_SIZE)
                .set();
        int headerRow = writeLegend(expenses.categories()) + HEADER_GAP;
        writeHeader(headerRow);
        int firstItemRow = headerRow + 1;
        List<ExpenseItem> items = expenses.items();
        for (int index = 0; index < items.size(); index++) {
            writeItem(firstItemRow + index, index, items.get(index), expenses);
        }
        int totalRow = firstItemRow + items.size();
        writeTotal(totalRow, firstItemRow, items.size());
        widths.trackNumber(COL_PRICE, MONEY_DISPLAY_LENGTH);
        widths.trackNumber(COL_MONTHLY, MONEY_DISPLAY_LENGTH);
        widths.apply(sheet);
        sheet.freezePane(0, firstItemRow);
        return totalRow;
    }

    /** @return baris terakhir yang dipakai legend (atau baris judul bila tanpa kategori) */
    private int writeLegend(List<Category> categories) {
        if (categories.isEmpty()) {
            return TITLE_ROW;
        }
        sheet.value(LEGEND_FIRST_ROW, COL_NO, labels.get(WorkbookLabel.LEGEND));
        XlsxStyle.BOLD.apply(sheet, LEGEND_FIRST_ROW, COL_NO);
        int lastRow = LEGEND_FIRST_ROW;
        for (int index = 0; index < categories.size(); index++) {
            Category category = categories.get(index);
            int row = LEGEND_FIRST_ROW + index / LEGEND_PER_ROW;
            int column = LEGEND_FIRST_COLUMN + (index % LEGEND_PER_ROW) * LEGEND_COLUMNS_PER_ENTRY;
            XlsxStyle.BORDERED.withFill(XlsxStyle.excelColor(category.color())).apply(sheet, row, column);
            sheet.value(row, column + 1, category.name());
            widths.track(column + 1, category.name());
            lastRow = row;
        }
        return lastRow;
    }

    private void writeHeader(int row) {
        for (int column = 0; column < HEADERS.size(); column++) {
            String text = labels.get(HEADERS.get(column));
            sheet.value(row, column, text);
            XlsxStyle.HEADER.apply(sheet, row, column);
            widths.track(column, text);
        }
    }

    private void writeItem(int row, int index, ExpenseItem item, ExpenseSheet expenses) {
        XlsxStyle base = rowStyle(index, item.status());
        sheet.value(row, COL_NO, index + 1);
        text(row, COL_NAME, item.name());
        text(row, COL_BRAND, item.brand());
        text(row, COL_NETTO, item.netto());
        sheet.value(row, COL_DURATION, item.duration());
        text(row, COL_UNIT, labels.get(WorkbookLabel.unit(item.unit())));
        sheet.value(row, COL_PRICE, item.price());
        XlsxCells.formula(sheet, row, COL_MONTHLY, monthlyFormula(row, item.unit()));
        text(row, COL_STATUS, labels.get(WorkbookLabel.status(item.status())));
        text(row, COL_NOTE, item.note());
        for (int column = COL_NAME; column <= COL_NOTE; column++) {
            boolean money = column == COL_PRICE || column == COL_MONTHLY;
            (money ? base.withFormat(XlsxStyle.RUPIAH) : base).apply(sheet, row, column);
        }
        String noFill = expenses.category(item.categoryId())
                .map(category -> XlsxStyle.excelColor(category.color()))
                .orElse(base.fill());
        base.withFill(noFill).withAlign(XlsxStyle.CENTER).apply(sheet, row, COL_NO);
    }

    /** Teks user selalu sel string, tidak pernah formula (Security Requirements #18). */
    private void text(int row, int column, String value) {
        sheet.value(row, column, value);
        widths.track(column, value);
    }

    private static XlsxStyle rowStyle(int index, ItemStatus status) {
        if (!status.counts()) {
            return XlsxStyle.BORDERED.withFill(XlsxStyle.INACTIVE_FILL).withFontColor(XlsxStyle.INACTIVE_FONT);
        }
        return index % 2 == 1 ? XlsxStyle.BORDERED.withFill(XlsxStyle.ZEBRA_FILL) : XlsxStyle.BORDERED;
    }

    /** {@code IF(E5>0,G5/(E5*faktor),0)}: tetap benar bila harga/durasi diubah di Excel. */
    private static String monthlyFormula(int row, DurationUnit unit) {
        String duration = XlsxCells.ref(row, COL_DURATION);
        String price = XlsxCells.ref(row, COL_PRICE);
        return "IF(" + duration + ">0," + price + "/(" + duration + "*" + monthsPerUnit(unit) + "),0)";
    }

    /** Bulan per satuan sebagai konstanta numerik; hari & minggu ditulis sebagai pecahan supaya eksak. */
    static String monthsPerUnit(DurationUnit unit) {
        String daysPerMonth = MonthlyCost.DAYS_PER_MONTH.toPlainString();
        return switch (unit) {
            case DAYS -> "1/" + daysPerMonth;
            case WEEKS -> MonthlyCost.DAYS_PER_WEEK.toPlainString() + "/" + daysPerMonth;
            case MONTHS -> "1";
            case YEARS -> MonthlyCost.MONTHS_PER_YEAR.toPlainString();
        };
    }

    private void writeTotal(int row, int firstItemRow, int itemCount) {
        String label = labels.get(WorkbookLabel.TOTAL_ACTIVE);
        sheet.value(row, COL_NO, label);
        sheet.range(row, COL_NO, row, COL_UNIT).merge();
        for (int column = COL_NO; column <= COL_NOTE; column++) {
            boolean money = column == COL_PRICE || column == COL_MONTHLY;
            (money ? XlsxStyle.TOTAL.withFormat(XlsxStyle.RUPIAH) : XlsxStyle.TOTAL).apply(sheet, row, column);
        }
        if (itemCount == 0) {
            sheet.value(row, COL_PRICE, 0);
            sheet.value(row, COL_MONTHLY, 0);
            return;
        }
        int lastItemRow = firstItemRow + itemCount - 1;
        String status = XlsxCells.range(firstItemRow, lastItemRow, COL_STATUS);
        String active = XlsxCells.text(labels.get(WorkbookLabel.STATUS_ACTIVE));
        for (int column : new int[] {COL_PRICE, COL_MONTHLY}) {
            String values = XlsxCells.range(firstItemRow, lastItemRow, column);
            XlsxCells.formula(sheet, row, column, "SUMIF(" + status + "," + active + "," + values + ")");
        }
    }
}
