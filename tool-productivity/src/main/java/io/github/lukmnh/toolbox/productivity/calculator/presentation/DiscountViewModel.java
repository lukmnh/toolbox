package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.productivity.calculator.application.CalculateDiscountUseCase;
import io.github.lukmnh.toolbox.productivity.calculator.domain.CalculatorInputException;
import io.github.lukmnh.toolbox.productivity.calculator.domain.DiscountRequest;
import io.github.lukmnh.toolbox.productivity.calculator.domain.DiscountResult;
import io.github.lukmnh.toolbox.productivity.calculator.domain.DiscountStep;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
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

/** State Diskon Ganda. Hitungan ringan, jadi dihitung ulang langsung di FX thread setiap input berubah. */
final class DiscountViewModel {

    static final String PRICE_KEY = "productivity.calculator.discount.price";
    static final String DISCOUNT_KEY = "productivity.calculator.discount.percent";
    static final String DEDUCTION_KEY = "productivity.calculator.discount.deduction";

    private final CalculateDiscountUseCase useCase;
    private final Notifier notifier;

    private final StringProperty price = new SimpleStringProperty("");
    /** Extractor: perubahan teks di baris mana pun ikut memicu listener list. */
    private final ObservableList<StringProperty> discounts =
            FXCollections.observableArrayList(row -> new Observable[] {row});

    private final StringProperty deduction = new SimpleStringProperty("");
    private final ObjectProperty<DiscountResult> result = new SimpleObjectProperty<>();
    private final StringProperty error = new SimpleStringProperty("");

    DiscountViewModel(CalculateDiscountUseCase useCase, Notifier notifier) {
        this.useCase = useCase;
        this.notifier = notifier;
        discounts.add(new SimpleStringProperty(""));
        price.addListener((observable, previous, current) -> recalculate());
        deduction.addListener((observable, previous, current) -> recalculate());
        discounts.addListener((Observable observable) -> recalculate());
    }

    StringProperty priceProperty() {
        return price;
    }

    ObservableList<StringProperty> discounts() {
        return discounts;
    }

    StringProperty deductionProperty() {
        return deduction;
    }

    ReadOnlyObjectProperty<DiscountResult> resultProperty() {
        return result;
    }

    ReadOnlyStringProperty errorProperty() {
        return error;
    }

    BooleanBinding canAddDiscount() {
        return Bindings.size(discounts).lessThan(DiscountRequest.MAX_DISCOUNTS);
    }

    void addDiscount() {
        if (discounts.size() < DiscountRequest.MAX_DISCOUNTS) {
            discounts.add(new SimpleStringProperty(""));
        }
    }

    void removeDiscount(StringProperty row) {
        discounts.remove(row);
    }

    void copySummary() {
        ClipboardActions.copy(summary(), notifier);
    }

    /** Ringkasan teks untuk clipboard; kosong bila belum ada hasil. */
    String summary() {
        DiscountResult current = result.get();
        if (current == null) {
            return "";
        }
        StringJoiner lines = new StringJoiner(System.lineSeparator());
        lines.add(Messages.format(
                "productivity.calculator.discount.summary.original",
                CalculatorMessages.money(current.originalPrice())));
        List<DiscountStep> steps = current.steps();
        for (int index = 0; index < steps.size(); index++) {
            lines.add(CalculatorMessages.step(index + 1, steps.get(index)));
        }
        lines.add(Messages.format(
                "productivity.calculator.discount.summary.final", CalculatorMessages.money(current.finalPrice())));
        lines.add(Messages.format(
                "productivity.calculator.discount.summary.saving", CalculatorMessages.money(current.totalSaving())));
        lines.add(Messages.format(
                "productivity.calculator.discount.summary.effective",
                CalculatorMessages.percent(current.effectiveDiscountPercent())));
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

    /** @return kosong selama harga awal belum diisi; baris diskon kosong dilewati */
    private Optional<DiscountRequest> request() {
        Optional<BigDecimal> original = FieldParser.decimal(price.get(), PRICE_KEY);
        if (original.isEmpty()) {
            return Optional.empty();
        }
        List<BigDecimal> percents = new ArrayList<>();
        for (StringProperty row : discounts) {
            FieldParser.decimal(row.get(), DISCOUNT_KEY).ifPresent(percents::add);
        }
        BigDecimal cut = FieldParser.decimal(deduction.get(), DEDUCTION_KEY).orElse(BigDecimal.ZERO);
        return Optional.of(new DiscountRequest(original.get(), percents, cut, CalculatorMessages.MONEY_SCALE));
    }

    private void fail(String message) {
        result.set(null);
        error.set(message);
    }
}
