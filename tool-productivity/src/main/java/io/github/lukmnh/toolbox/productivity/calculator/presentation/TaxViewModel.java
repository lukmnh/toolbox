package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Debounce;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.productivity.calculator.application.CalculateTaxUseCase;
import io.github.lukmnh.toolbox.productivity.calculator.domain.CalculatorInputException;
import io.github.lukmnh.toolbox.productivity.calculator.domain.TaxMode;
import io.github.lukmnh.toolbox.productivity.calculator.domain.TaxRequest;
import io.github.lukmnh.toolbox.productivity.calculator.domain.TaxResult;
import io.github.lukmnh.toolbox.productivity.shared.domain.RupiahFormat;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

/** State Kalkulator Pajak/PPN. Dihitung ulang (debounce) setiap nominal, tarif, atau mode berubah. */
final class TaxViewModel {

    static final String AMOUNT_KEY = "productivity.calculator.tax.amount";
    static final String RATE_KEY = "productivity.calculator.tax.rate";
    static final Duration RECALCULATE_DELAY = Duration.millis(200);

    private final CalculateTaxUseCase useCase;
    private final Notifier notifier;
    private final Debounce debounce;

    private final StringProperty amount = new SimpleStringProperty("");
    private final StringProperty rate =
            new SimpleStringProperty(RupiahFormat.plain(TaxRequest.DEFAULT_RATE_PERCENT, 0));
    private final ObjectProperty<TaxMode> mode = new SimpleObjectProperty<>(TaxMode.EXCLUSIVE);
    private final ObjectProperty<TaxResult> result = new SimpleObjectProperty<>();
    private final StringProperty error = new SimpleStringProperty("");

    TaxViewModel(CalculateTaxUseCase useCase, Notifier notifier) {
        this.useCase = useCase;
        this.notifier = notifier;
        this.debounce = new Debounce(RECALCULATE_DELAY, this::recalculate);
        for (StringProperty input : List.of(amount, rate)) {
            input.addListener((observable, previous, current) -> debounce.trigger());
        }
        mode.addListener((observable, previous, current) -> debounce.trigger());
    }

    StringProperty amountProperty() {
        return amount;
    }

    StringProperty rateProperty() {
        return rate;
    }

    ObjectProperty<TaxMode> modeProperty() {
        return mode;
    }

    ReadOnlyObjectProperty<TaxResult> resultProperty() {
        return result;
    }

    ReadOnlyStringProperty errorProperty() {
        return error;
    }

    void copySummary() {
        ClipboardActions.copy(summary(), notifier);
    }

    void dispose() {
        debounce.stop();
    }

    /** Ringkasan teks untuk clipboard; kosong bila belum ada hasil. */
    String summary() {
        TaxResult current = result.get();
        if (current == null) {
            return "";
        }
        StringJoiner lines = new StringJoiner(System.lineSeparator());
        lines.add(
                Messages.format("productivity.calculator.tax.summary.base", CalculatorMessages.money(current.base())));
        lines.add(Messages.format(
                "productivity.calculator.tax.summary.tax",
                CalculatorMessages.percent(current.ratePercent()),
                CalculatorMessages.money(current.tax())));
        lines.add(Messages.format(
                "productivity.calculator.tax.summary.total", CalculatorMessages.money(current.total())));
        return lines.toString();
    }

    private void recalculate() {
        try {
            result.set(request().map(useCase::execute).orElse(null));
            error.set("");
        } catch (InvalidFieldException e) {
            fail(e.getMessage());
        } catch (CalculatorInputException e) {
            fail(CalculatorMessages.problem(e.problem()));
        }
    }

    /** @return kosong selama nominal atau tarif belum diisi */
    private Optional<TaxRequest> request() {
        Optional<BigDecimal> amountValue = FieldParser.decimal(amount.get(), AMOUNT_KEY);
        Optional<BigDecimal> rateValue = FieldParser.decimal(rate.get(), RATE_KEY);
        if (amountValue.isEmpty() || rateValue.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(
                new TaxRequest(amountValue.get(), rateValue.get(), mode.get(), CalculatorMessages.MONEY_SCALE));
    }

    private void fail(String message) {
        result.set(null);
        error.set(message);
    }
}
