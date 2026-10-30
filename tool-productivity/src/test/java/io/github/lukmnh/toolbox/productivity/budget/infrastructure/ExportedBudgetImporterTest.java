package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.report.PdfReport;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetFileTooLargeException;
import io.github.lukmnh.toolbox.productivity.budget.domain.Category;
import io.github.lukmnh.toolbox.productivity.budget.domain.DurationUnit;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseItem;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseSheet;
import io.github.lukmnh.toolbox.productivity.budget.domain.Income;
import io.github.lukmnh.toolbox.productivity.budget.domain.InvalidBudgetFileException;
import io.github.lukmnh.toolbox.productivity.budget.domain.InvalidBudgetFileException.Problem;
import io.github.lukmnh.toolbox.productivity.budget.domain.ItemStatus;
import io.github.lukmnh.toolbox.productivity.budget.domain.WorkbookLabels;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.SheetVisibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportedBudgetImporterTest {

    /** Cukup banyak item supaya data Base64 terpecah ke beberapa sel. */
    private static final int ITEM_COUNT = 300;

    private static final WorkbookLabels LABELS = WorkbookLabels.from(Enum::name);

    private final BudgetJsonCodec codec = new BudgetJsonCodec();
    private final ExportedBudgetImporter importer = new ExportedBudgetImporter(codec);

    @TempDir
    Path directory;

    @Test
    void excelExportRoundTripsToEqualBudget() throws IOException {
        Budget budget = sampleBudget();
        Path xlsx = directory.resolve("budget.xlsx");

        new FastExcelBudgetWorkbook(codec).write(budget, LABELS, xlsx);

        assertEquals(budget, importer.read(xlsx));
        try (ReadableWorkbook workbook = new ReadableWorkbook(xlsx.toFile())) {
            assertEquals(
                    SheetVisibility.VERY_HIDDEN,
                    workbook.findSheet(XlsxBudgetData.SHEET_NAME).orElseThrow().getVisibility());
            assertTrue(workbook.findSheet(XlsxBudgetData.SHEET_NAME)
                            .orElseThrow()
                            .read()
                            .size()
                    > 2);
        }
    }

    @Test
    void userSheetNamedLikeDataSheetDoesNotCollide() {
        Budget budget = new Budget(
                "Collision",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                List.of(),
                List.of(new ExpenseSheet("sheet-x", XlsxBudgetData.SHEET_NAME, List.of(), List.of())));
        Path xlsx = directory.resolve("collision.xlsx");

        new FastExcelBudgetWorkbook(codec).write(budget, LABELS, xlsx);

        assertEquals(budget, importer.read(xlsx));
    }

    @Test
    void pdfExportRoundTripsToEqualBudget() throws IOException {
        Budget budget = sampleBudget();
        Path pdf = directory.resolve("budget.pdf");

        new PdfBudgetReport(codec).write(budget, LABELS, pdf);

        assertEquals(budget, importer.read(pdf));
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Rumah Tangga"));
            assertTrue(text.contains("Skincare"));
            assertTrue(text.contains("Sabun"));
        }
    }

    @Test
    void rejectsExcelWithoutToolboxData() throws IOException {
        Path xlsx = directory.resolve("foreign.xlsx");
        try (OutputStream output = Files.newOutputStream(xlsx)) {
            Workbook workbook = new Workbook(output, "Other", "1.0");
            workbook.newWorksheet("Sheet1").value(0, 0, "hello");
            workbook.finish();
        }

        assertProblem(Problem.NOT_TOOLBOX_EXPORT, xlsx);
    }

    @Test
    void rejectsExcelWithUnknownDataVersion() throws IOException {
        Path xlsx = directory.resolve("future.xlsx");
        try (OutputStream output = Files.newOutputStream(xlsx)) {
            Workbook workbook = new Workbook(output, "Toolbox", "1.0");
            workbook.newWorksheet("Summary").value(0, 0, "x");
            var data = workbook.newWorksheet(XlsxBudgetData.SHEET_NAME);
            data.inlineString(0, 0, XlsxBudgetData.FORMAT);
            data.inlineString(0, 1, "99");
            workbook.finish();
        }

        assertProblem(Problem.UNSUPPORTED_VERSION, xlsx);
    }

    @Test
    void rejectsPdfWithoutToolboxData() throws IOException {
        Path pdf = directory.resolve("foreign.pdf");
        PdfReport.titled("Other report").paragraph("No data").writeTo(pdf);

        assertProblem(Problem.NOT_TOOLBOX_EXPORT, pdf);
    }

    @Test
    void rejectsUnknownAndCorruptFiles() throws IOException {
        Path json = Files.writeString(directory.resolve("old.json"), "{\"version\":1}");
        Path zip = directory.resolve("corrupt.xlsx");
        Files.write(zip, concat(new byte[] {'P', 'K', 3, 4}, "garbage".getBytes(StandardCharsets.US_ASCII)));
        Path pdf = Files.writeString(directory.resolve("corrupt.pdf"), "%PDF-1.7 garbage");

        assertProblem(Problem.NOT_TOOLBOX_EXPORT, json);
        assertProblem(Problem.UNREADABLE_FILE, zip);
        assertProblem(Problem.UNREADABLE_FILE, pdf);
    }

    @Test
    void rejectsOversizeFileBeforeReading() throws IOException {
        Path xlsx = directory.resolve("huge.xlsx");
        try (RandomAccessFile file = new RandomAccessFile(xlsx.toFile(), "rw")) {
            file.setLength(SecurityLimits.BUDGET_EXPORT_MAX_FILE_BYTES + 1);
        }

        BudgetFileTooLargeException error = assertThrows(BudgetFileTooLargeException.class, () -> importer.read(xlsx));
        assertEquals(SecurityLimits.BUDGET_EXPORT_MAX_FILE_BYTES, error.maxBytes());
    }

    @Test
    void rejectsOversizeEmbeddedData() throws IOException {
        Path pdf = directory.resolve("big-data.pdf");
        byte[] data = new byte[(int) SecurityLimits.BUDGET_IMPORT_MAX_FILE_BYTES + 1];
        PdfReport.titled("Big").attach(PdfBudgetReport.ATTACHMENT, data).writeTo(pdf);

        BudgetFileTooLargeException error = assertThrows(BudgetFileTooLargeException.class, () -> importer.read(pdf));
        assertEquals(SecurityLimits.BUDGET_IMPORT_MAX_FILE_BYTES, error.maxBytes());
    }

    private void assertProblem(Problem expected, Path file) {
        InvalidBudgetFileException error = assertThrows(InvalidBudgetFileException.class, () -> importer.read(file));
        assertEquals(expected, error.problem());
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static Budget sampleBudget() {
        Category basic = new Category("cat-basic", "Basic", "#A5F3FC");
        List<ExpenseItem> items = new ArrayList<>();
        for (int index = 0; index < ITEM_COUNT; index++) {
            items.add(new ExpenseItem(
                    "item-" + index,
                    index == 0 ? "Sabun" : "Barang " + index + " — ünïcödé 🧴",
                    "Brand " + index,
                    "100 ml",
                    index % 3 == 0 ? "" : basic.id(),
                    new BigDecimal("15000").add(BigDecimal.valueOf(index)),
                    new BigDecimal("1.5"),
                    DurationUnit.values()[index % DurationUnit.values().length],
                    index % 5 == 0 ? ItemStatus.INACTIVE : ItemStatus.ACTIVE,
                    "  catatan dengan  spasi ganda\nbaris baru " + index));
        }
        return new Budget(
                "Rumah Tangga",
                Instant.parse("2026-01-01T08:00:00Z"),
                Instant.parse("2026-02-01T09:30:00Z"),
                List.of(new Income("inc-1", "Gaji", new BigDecimal("12500000.50"), "")),
                List.of(
                        new ExpenseSheet("sheet-1", "Skincare", List.of(basic), items),
                        new ExpenseSheet("sheet-2", "Transport", List.of(), List.of())));
    }
}
