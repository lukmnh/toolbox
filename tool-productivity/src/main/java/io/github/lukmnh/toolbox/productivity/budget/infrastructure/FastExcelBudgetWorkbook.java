package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetExportWriter;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetStorageException;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseSheet;
import io.github.lukmnh.toolbox.productivity.budget.domain.WorkbookLabel;
import io.github.lukmnh.toolbox.productivity.budget.domain.WorkbookLabels;
import io.github.lukmnh.toolbox.productivity.budget.infrastructure.SummarySheetWriter.SheetTotalRef;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

/**
 * Export {@code .xlsx} dengan FastExcel (streaming, tanpa Apache POI): sheet Ringkasan, satu sheet per daftar, lalu
 * sheet data tersembunyi untuk import ulang ({@link XlsxBudgetData}).
 */
public final class FastExcelBudgetWorkbook implements BudgetExportWriter {

    private static final String APPLICATION_NAME = "Toolbox";
    /** FastExcel mewajibkan format versi {@code angka.angka}. */
    private static final String APPLICATION_VERSION = "1.0";

    private static final System.Logger LOG = System.getLogger(FastExcelBudgetWorkbook.class.getName());
    private static final String RAW_PREFIX = ".budget-raw-";
    private static final String RAW_SUFFIX = ".xlsx";

    private final BudgetJsonCodec codec;

    public FastExcelBudgetWorkbook(BudgetJsonCodec codec) {
        this.codec = codec;
    }

    /** Ditulis ke file mentah di folder yang sama, lalu di-repack ke {@code target} (lihat {@link OpcRepacker}). */
    @Override
    public void write(Budget budget, WorkbookLabels labels, Path target) {
        Path raw = null;
        try {
            raw = Files.createTempFile(target.toAbsolutePath().getParent(), RAW_PREFIX, RAW_SUFFIX);
            try (OutputStream output = Files.newOutputStream(raw)) {
                Workbook workbook = new Workbook(output, APPLICATION_NAME, APPLICATION_VERSION);
                writeSheets(workbook, budget, labels);
                XlsxBudgetData.write(workbook, codec.toBytes(budget));
                workbook.finish();
            }
            OpcRepacker.repack(raw, target);
        } catch (IOException e) {
            throw new BudgetStorageException(target.getFileName().toString(), e);
        } finally {
            deleteRaw(raw);
        }
    }

    private static void deleteRaw(Path raw) {
        if (raw == null) {
            return;
        }
        try {
            Files.deleteIfExists(raw);
        } catch (IOException e) {
            // Sisa file mentah ada di folder temp aplikasi yang dibersihkan saat dispose/exit; cukup dicatat
            LOG.log(System.Logger.Level.WARNING, "Gagal menghapus file xlsx mentah " + raw.getFileName(), e);
        }
    }

    private static void writeSheets(Workbook workbook, Budget budget, WorkbookLabels labels) throws IOException {
        List<String> names = SheetNames.assign(
                Stream.concat(
                                Stream.of(labels.get(WorkbookLabel.SUMMARY_SHEET)),
                                budget.sheets().stream().map(ExpenseSheet::name))
                        .toList(),
                labels.get(WorkbookLabel.DEFAULT_SHEET));
        // Sheet Ringkasan dibuat pertama supaya menjadi tab pertama, tapi diisi terakhir karena butuh baris total
        Worksheet summary = workbook.newWorksheet(names.getFirst());
        List<SheetTotalRef> totals = new ArrayList<>();
        for (int index = 0; index < budget.sheets().size(); index++) {
            ExpenseSheet expenses = budget.sheets().get(index);
            Worksheet worksheet = workbook.newWorksheet(names.get(index + 1));
            int totalRow = ExpenseSheetWriter.write(worksheet, expenses, labels);
            // Nama final dari FastExcel dipakai di formula, jaga-jaga bila library ikut menyesuaikan nama
            totals.add(new SheetTotalRef(expenses.name(), worksheet.getName(), totalRow));
            worksheet.finish();
        }
        SummarySheetWriter.write(summary, budget, totals, labels);
        summary.finish();
    }
}
