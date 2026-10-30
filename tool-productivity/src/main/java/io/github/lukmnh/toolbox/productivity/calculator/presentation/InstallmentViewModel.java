package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.productivity.calculator.domain.AdminFee;
import io.github.lukmnh.toolbox.productivity.calculator.domain.AmortizationRow;
import io.github.lukmnh.toolbox.productivity.calculator.domain.CalculatorInputException;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentMethod;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentRequest;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentSchedule;
import io.github.lukmnh.toolbox.productivity.calculator.domain.ReportDocument;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** State Simulasi Cicilan. Jadwal (maks 600 baris) dihitung di FX thread; hanya export CSV/PDF yang ke background. */
final class InstallmentViewModel {

    static final String PRINCIPAL_KEY = "productivity.calculator.installment.principal";
    static final String TENOR_KEY = "productivity.calculator.installment.tenor";
    static final String RATE_KEY = "productivity.calculator.installment.rate";
    static final String FEE_KEY = "productivity.calculator.installment.fee";

    /** Judul kolom tabel amortisasi; dipakai juga sebagai header CSV supaya keduanya selalu sama. */
    static final List<String> COLUMN_KEYS = List.of(
            "productivity.calculator.schedule.month",
            "productivity.calculator.schedule.payment",
            "productivity.calculator.schedule.principal",
            "productivity.calculator.schedule.interest",
            "productivity.calculator.schedule.remaining");

    private static final String CSV_NAME = "installment-schedule.csv";
    private static final String CSV_SUFFIX = ".csv";
    private static final String PDF_NAME = "installment-simulation.pdf";
    private static final String PDF_SUFFIX = ".pdf";
    /** Key ringkasan = key label tile + akhiran ini (label dengan placeholder nominal). */
    private static final String VALUE_SUFFIX = ".value";

    private final CalculatorUseCases useCases;
    private final ToolContext context;
    private final FileExport fileExport;

    private final StringProperty principal = new SimpleStringProperty("");
    private final StringProperty tenor = new SimpleStringProperty("");
    private final StringProperty rate = new SimpleStringProperty("");
    private final ObjectProperty<InstallmentMethod> method = new SimpleObjectProperty<>(InstallmentMethod.ANNUITY);
    private final ObjectProperty<FeeMode> feeMode = new SimpleObjectProperty<>(FeeMode.NOMINAL);
    private final StringProperty fee = new SimpleStringProperty("");
    private final ObjectProperty<InstallmentSchedule> schedule = new SimpleObjectProperty<>();
    private final ObservableList<AmortizationRow> rows = FXCollections.observableArrayList();
    private final StringProperty error = new SimpleStringProperty("");

    private InstallmentRequest lastRequest;

    InstallmentViewModel(CalculatorUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.fileExport = new FileExport(context, tempFiles);
        for (var input : List.of(principal, tenor, rate, fee)) {
            input.addListener((observable, previous, current) -> recalculate());
        }
        method.addListener((observable, previous, current) -> recalculate());
        feeMode.addListener((observable, previous, current) -> recalculate());
    }

    StringProperty principalProperty() {
        return principal;
    }

    StringProperty tenorProperty() {
        return tenor;
    }

    StringProperty rateProperty() {
        return rate;
    }

    ObjectProperty<InstallmentMethod> methodProperty() {
        return method;
    }

    ObjectProperty<FeeMode> feeModeProperty() {
        return feeMode;
    }

    StringProperty feeProperty() {
        return fee;
    }

    ReadOnlyObjectProperty<InstallmentSchedule> scheduleProperty() {
        return schedule;
    }

    ObservableList<AmortizationRow> rows() {
        return FXCollections.unmodifiableObservableList(rows);
    }

    ReadOnlyStringProperty errorProperty() {
        return error;
    }

    BooleanBinding canExport() {
        return schedule.isNotNull().and(fileExport.busyProperty().not());
    }

    void copySummary() {
        ClipboardActions.copy(summary(), context.notifier());
    }

    void downloadCsv() {
        exportCsv(context.downloads()::download);
    }

    void saveCsvAs() {
        exportCsv(context.downloads()::saveAs);
    }

    void downloadPdf() {
        exportPdf(context.downloads()::download);
    }

    void savePdfAs() {
        exportPdf(context.downloads()::saveAs);
    }

    void dispose() {
        fileExport.dispose();
    }

    /** Ringkasan teks untuk clipboard, termasuk disclaimer; kosong bila belum ada hasil. */
    String summary() {
        InstallmentSchedule current = schedule.get();
        if (current == null || lastRequest == null) {
            return "";
        }
        StringJoiner lines = new StringJoiner(System.lineSeparator());
        lines.add(Messages.format(
                "productivity.calculator.installment.summary.title", CalculatorMessages.method(current.method())));
        lines.add(Messages.format(
                "productivity.calculator.installment.summary.principal",
                CalculatorMessages.money(current.principal())));
        lines.add(Messages.format(
                "productivity.calculator.installment.summary.terms",
                current.tenorMonths(),
                CalculatorMessages.percent(lastRequest.annualRatePercent())));
        lines.add(Messages.format(
                firstPaymentKey(current) + VALUE_SUFFIX, CalculatorMessages.money(current.firstPayment())));
        if (hasDifferentLastPayment(current)) {
            lines.add(Messages.format(
                    "productivity.calculator.installment.lastPayment" + VALUE_SUFFIX,
                    CalculatorMessages.money(current.lastPayment())));
        }
        lines.add(Messages.format(
                "productivity.calculator.installment.summary.interest",
                CalculatorMessages.money(current.totalInterest())));
        lines.add(Messages.format(
                "productivity.calculator.installment.summary.fee", CalculatorMessages.money(current.fee())));
        lines.add(Messages.format(
                "productivity.calculator.installment.summary.total", CalculatorMessages.money(current.totalCost())));
        lines.add(Messages.get("productivity.calculator.installment.disclaimer"));
        return lines.toString();
    }

    /** Metode efektif selalu menurun, jadi labelnya "bulan pertama" bukan "per bulan". */
    static String firstPaymentKey(InstallmentSchedule current) {
        return current.method() == InstallmentMethod.EFFECTIVE
                ? "productivity.calculator.installment.firstPayment"
                : "productivity.calculator.installment.monthlyPayment";
    }

    /** Bulan terakhir bisa beda karena koreksi pembulatan (anuitas/flat) atau karena metodenya menurun (efektif). */
    static boolean hasDifferentLastPayment(InstallmentSchedule current) {
        return current.firstPayment().compareTo(current.lastPayment()) != 0;
    }

    private void recalculate() {
        try {
            Optional<InstallmentRequest> request = request();
            lastRequest = request.orElse(null);
            show(request.map(useCases.installment()::execute).orElse(null));
            error.set("");
        } catch (InvalidFieldException e) {
            fail(e.getMessage());
        } catch (CalculatorInputException e) {
            fail(CalculatorMessages.problem(e.problem()));
        }
    }

    /** @return kosong selama pokok, tenor, atau bunga belum diisi */
    private Optional<InstallmentRequest> request() {
        Optional<BigDecimal> principalValue = FieldParser.decimal(principal.get(), PRINCIPAL_KEY);
        Optional<Integer> months = FieldParser.wholeNumber(tenor.get(), TENOR_KEY);
        Optional<BigDecimal> annualRate = FieldParser.decimal(rate.get(), RATE_KEY);
        AdminFee adminFee = FieldParser.decimal(fee.get(), FEE_KEY)
                .map(value -> feeMode.get().toFee(value))
                .orElse(AdminFee.NONE);
        if (principalValue.isEmpty() || months.isEmpty() || annualRate.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new InstallmentRequest(
                principalValue.get(),
                months.get(),
                annualRate.get(),
                method.get(),
                adminFee,
                CalculatorMessages.MONEY_SCALE));
    }

    private void show(InstallmentSchedule result) {
        schedule.set(result);
        rows.setAll(result == null ? List.of() : result.rows());
    }

    private void fail(String message) {
        lastRequest = null;
        show(null);
        error.set(message);
    }

    private void exportCsv(BiConsumer<Path, String> deliver) {
        InstallmentSchedule current = schedule.get();
        if (current == null) {
            return;
        }
        List<String> header = COLUMN_KEYS.stream().map(Messages::get).toList();
        fileExport.run(
                CSV_SUFFIX,
                target -> useCases.export().execute(current, header, target),
                file -> deliver.accept(file, CSV_NAME));
    }

    private void exportPdf(BiConsumer<Path, String> deliver) {
        InstallmentSchedule current = schedule.get();
        if (current == null || lastRequest == null) {
            return;
        }
        // Teks disusun di FX thread (Messages/format), hanya penulisan file yang ke background
        ReportDocument document = CalculatorReports.installment(current, lastRequest);
        fileExport.run(
                PDF_SUFFIX,
                target -> useCases.exportPdf().execute(document, target),
                file -> deliver.accept(file, PDF_NAME));
    }
}
