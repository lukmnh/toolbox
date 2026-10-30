package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Debounce;
import io.github.lukmnh.toolbox.productivity.calculator.domain.CalculatorInputException;
import io.github.lukmnh.toolbox.productivity.calculator.domain.Invoice;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceLine;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceParty;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoicePricing;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceTotals;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceType;
import io.github.lukmnh.toolbox.productivity.calculator.domain.ReportDocument;
import io.github.lukmnh.toolbox.productivity.calculator.domain.TaxRequest;
import io.github.lukmnh.toolbox.productivity.shared.domain.RupiahFormat;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * State Invoice/Kuitansi. Total dihitung ulang (debounce) saat baris, diskon, atau pajak berubah; validasi lengkap
 * (nomor, penjual, minimal satu baris) baru dijalankan saat export PDF.
 */
final class InvoiceViewModel {

    static final String NUMBER_KEY = "productivity.calculator.invoice.number";
    static final String SELLER_KEY = "productivity.calculator.invoice.seller";
    static final String CUSTOMER_KEY = "productivity.calculator.invoice.customer";
    static final String QUANTITY_KEY = "productivity.calculator.invoice.column.quantity";
    static final String UNIT_PRICE_KEY = "productivity.calculator.invoice.column.unitPrice";
    static final String DISCOUNT_KEY = "productivity.calculator.invoice.discount";
    static final String TAX_KEY = "productivity.calculator.invoice.tax";

    /** Kolom tabel item di PDF: no, deskripsi, qty, harga satuan, jumlah. */
    static final List<String> PDF_COLUMN_KEYS = List.of(
            "productivity.calculator.invoice.column.number",
            "productivity.calculator.invoice.column.description",
            QUANTITY_KEY,
            UNIT_PRICE_KEY,
            "productivity.calculator.invoice.column.amount");

    private static final String PDF_SUFFIX = ".pdf";
    private static final Pattern UNSAFE_FILE_CHARACTERS = Pattern.compile("[^A-Za-z0-9._-]+");

    private final CalculatorUseCases useCases;
    private final ToolContext context;
    private final FileExport fileExport;
    private final Debounce debounce;

    private final ObjectProperty<InvoiceType> type = new SimpleObjectProperty<>(InvoiceType.INVOICE);
    private final StringProperty number = new SimpleStringProperty("");
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now());
    private final StringProperty sellerName = new SimpleStringProperty("");
    private final StringProperty sellerDetails = new SimpleStringProperty("");
    private final StringProperty customerName = new SimpleStringProperty("");
    private final StringProperty customerDetails = new SimpleStringProperty("");
    private final ObservableList<InvoiceLineInput> lines = FXCollections.observableArrayList(InvoiceLineInput::inputs);
    private final StringProperty discount = new SimpleStringProperty("");
    private final StringProperty tax = new SimpleStringProperty(RupiahFormat.plain(TaxRequest.DEFAULT_RATE_PERCENT, 0));
    private final StringProperty notes = new SimpleStringProperty("");
    private final ObjectProperty<InvoiceTotals> totals = new SimpleObjectProperty<>();
    private final StringProperty error = new SimpleStringProperty("");
    private final StringProperty exportError = new SimpleStringProperty("");

    /** Pricing yang menghasilkan {@link #totals}; dipakai ulang saat export supaya angka PDF = angka di layar. */
    private InvoicePricing lastPricing;

    InvoiceViewModel(CalculatorUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.fileExport = new FileExport(context, tempFiles);
        this.debounce = new Debounce(TaxViewModel.RECALCULATE_DELAY, this::recalculate);
        lines.add(new InvoiceLineInput());
        lines.addListener((Observable observable) -> debounce.trigger());
        discount.addListener((observable, previous, current) -> debounce.trigger());
        tax.addListener((observable, previous, current) -> debounce.trigger());
        // Field header tidak memengaruhi total, cukup hapus pesan export yang sudah basi
        for (Observable header :
                List.of(type, number, date, sellerName, sellerDetails, customerName, customerDetails, notes)) {
            header.addListener(observable -> exportError.set(""));
        }
        recalculate();
    }

    ObjectProperty<InvoiceType> typeProperty() {
        return type;
    }

    StringProperty numberProperty() {
        return number;
    }

    ObjectProperty<LocalDate> dateProperty() {
        return date;
    }

    StringProperty sellerNameProperty() {
        return sellerName;
    }

    StringProperty sellerDetailsProperty() {
        return sellerDetails;
    }

    StringProperty customerNameProperty() {
        return customerName;
    }

    StringProperty customerDetailsProperty() {
        return customerDetails;
    }

    ObservableList<InvoiceLineInput> lines() {
        return lines;
    }

    StringProperty discountProperty() {
        return discount;
    }

    StringProperty taxProperty() {
        return tax;
    }

    StringProperty notesProperty() {
        return notes;
    }

    ReadOnlyObjectProperty<InvoiceTotals> totalsProperty() {
        return totals;
    }

    ReadOnlyStringProperty errorProperty() {
        return error;
    }

    ReadOnlyStringProperty exportErrorProperty() {
        return exportError;
    }

    BooleanBinding canAddLine() {
        return Bindings.size(lines).lessThan(InvoicePricing.MAX_LINES);
    }

    BooleanBinding canExport() {
        return totals.isNotNull().and(fileExport.busyProperty().not());
    }

    void addLine() {
        if (lines.size() < InvoicePricing.MAX_LINES) {
            lines.add(new InvoiceLineInput());
        }
    }

    /** Minimal satu baris tetap ada supaya form tidak pernah kosong tanpa tempat mengetik. */
    void removeLine(InvoiceLineInput line) {
        if (lines.size() > 1) {
            lines.remove(line);
        }
    }

    void copySummary() {
        ClipboardActions.copy(summary(), context.notifier());
    }

    void downloadPdf() {
        exportPdf(context.downloads()::download);
    }

    void savePdfAs() {
        exportPdf(context.downloads()::saveAs);
    }

    void dispose() {
        debounce.stop();
        fileExport.dispose();
    }

    /** Ringkasan total untuk clipboard; kosong bila total belum valid. */
    String summary() {
        InvoiceTotals current = totals.get();
        if (current == null) {
            return "";
        }
        StringJoiner text = new StringJoiner(System.lineSeparator());
        CalculatorReports.totalRows(current).forEach((label, value) -> text.add(label + ": " + value));
        return text.toString();
    }

    private void recalculate() {
        exportError.set("");
        Map<InvoiceLineInput, InvoiceLine> parsed = new IdentityHashMap<>();
        try {
            for (int index = 0; index < lines.size(); index++) {
                InvoiceLineInput input = lines.get(index);
                if (!input.isBlank()) {
                    parsed.put(input, parseLine(index + 1, input));
                }
            }
            InvoicePricing pricing = new InvoicePricing(
                    lines.stream().filter(parsed::containsKey).map(parsed::get).toList(),
                    FieldParser.decimal(discount.get(), DISCOUNT_KEY).orElse(BigDecimal.ZERO),
                    FieldParser.decimal(tax.get(), TAX_KEY).orElse(BigDecimal.ZERO),
                    CalculatorMessages.MONEY_SCALE);
            InvoiceTotals result = useCases.invoice().execute(pricing);
            lastPricing = pricing;
            totals.set(result);
            showLineAmounts(parsed);
            error.set("");
        } catch (InvalidFieldException e) {
            fail(e.getMessage());
        } catch (CalculatorInputException e) {
            fail(CalculatorMessages.problem(e.problem()));
        }
    }

    /** @throws InvalidFieldException dengan nomor baris supaya user tahu baris mana yang salah */
    private static InvoiceLine parseLine(int number, InvoiceLineInput input) {
        try {
            BigDecimal quantity = FieldParser.decimal(input.quantityProperty().get(), QUANTITY_KEY)
                    .orElse(BigDecimal.ONE);
            BigDecimal unitPrice = FieldParser.decimal(input.unitPriceProperty().get(), UNIT_PRICE_KEY)
                    .orElse(BigDecimal.ZERO);
            return new InvoiceLine(input.descriptionProperty().get(), quantity, unitPrice);
        } catch (InvalidFieldException e) {
            throw new InvalidFieldException(
                    Messages.format("productivity.calculator.invoice.lineError", number, e.getMessage()));
        } catch (CalculatorInputException e) {
            throw new InvalidFieldException(Messages.format(
                    "productivity.calculator.invoice.lineError", number, CalculatorMessages.problem(e.problem())));
        }
    }

    private void showLineAmounts(Map<InvoiceLineInput, InvoiceLine> parsed) {
        for (InvoiceLineInput input : lines) {
            InvoiceLine line = parsed.get(input);
            input.setAmount(line == null ? "" : CalculatorMessages.money(line.amount(CalculatorMessages.MONEY_SCALE)));
        }
    }

    private void fail(String message) {
        lastPricing = null;
        totals.set(null);
        lines.forEach(input -> input.setAmount(""));
        error.set(message);
    }

    private void exportPdf(BiConsumer<Path, String> deliver) {
        // Klik bisa datang sebelum debounce selesai: hitung sekarang supaya PDF memuat ketikan terakhir
        debounce.stop();
        recalculate();
        InvoiceTotals current = totals.get();
        if (current == null || lastPricing == null) {
            return;
        }
        Invoice invoice;
        try {
            invoice = new Invoice(
                    type.get(),
                    number.get(),
                    date.get() == null ? LocalDate.now() : date.get(),
                    new InvoiceParty(sellerName.get(), sellerDetails.get()),
                    new InvoiceParty(customerName.get(), customerDetails.get()),
                    lastPricing,
                    notes.get());
        } catch (CalculatorInputException e) {
            exportError.set(CalculatorMessages.problem(e.problem()));
            return;
        }
        exportError.set("");
        ReportDocument document = CalculatorReports.invoice(invoice, current);
        String fileName = fileName(invoice);
        fileExport.run(
                PDF_SUFFIX,
                target -> useCases.exportPdf().execute(document, target),
                file -> deliver.accept(file, fileName));
    }

    /** {@code invoice-INV-001.pdf}; karakter yang tidak aman untuk nama file diganti tanda hubung. */
    static String fileName(Invoice invoice) {
        String safeNumber =
                UNSAFE_FILE_CHARACTERS.matcher(invoice.number()).replaceAll("-").replaceAll("^[-.]+|[-.]+$", "");
        String prefix = invoice.type().name().toLowerCase(Locale.ROOT);
        return (safeNumber.isEmpty() ? prefix : prefix + "-" + safeNumber) + PDF_SUFFIX;
    }
}
