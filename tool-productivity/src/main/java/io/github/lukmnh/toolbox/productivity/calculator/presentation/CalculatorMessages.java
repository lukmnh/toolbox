package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.productivity.calculator.domain.DiscountRequest;
import io.github.lukmnh.toolbox.productivity.calculator.domain.DiscountStep;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InputProblem;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentMethod;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentRequest;
import io.github.lukmnh.toolbox.productivity.calculator.domain.ScheduleExportException;
import io.github.lukmnh.toolbox.productivity.shared.domain.RupiahFormat;
import java.math.BigDecimal;
import java.util.Locale;
import javafx.stage.Window;

/** Format uang/persen dan pemetaan error Kalkulator ke teks berbahasa Indonesia. */
final class CalculatorMessages {

    /**
     * Settings belum punya opsi 0/2 desimal, jadi sementara Rupiah bulat. Dipakai juga sebagai skala pembulatan
     * hitungan supaya angka yang tampil = angka yang dihitung.
     */
    static final int MONEY_SCALE = RupiahFormat.NO_DECIMALS;

    private static final int PERCENT_FRACTION_DIGITS = 2;

    private CalculatorMessages() {}

    static String money(BigDecimal amount) {
        return RupiahFormat.format(amount, MONEY_SCALE);
    }

    static String percent(BigDecimal value) {
        return Messages.format("productivity.calculator.percent", RupiahFormat.plain(value, PERCENT_FRACTION_DIGITS));
    }

    static String method(InstallmentMethod method) {
        return Messages.get("productivity.calculator.method." + method.name().toLowerCase(Locale.ROOT));
    }

    static String methodHint(InstallmentMethod method) {
        return Messages.get("productivity.calculator.method." + method.name().toLowerCase(Locale.ROOT) + ".hint");
    }

    /** @param number urutan tahap mulai 1 */
    static String step(int number, DiscountStep step) {
        return switch (step) {
            case DiscountStep.Percent percentStep ->
                Messages.format(
                        "productivity.calculator.discount.step.percent",
                        number,
                        percent(percentStep.percent()),
                        money(percentStep.cut()),
                        money(percentStep.priceAfter()));
            case DiscountStep.Nominal nominalStep ->
                Messages.format(
                        "productivity.calculator.discount.step.nominal",
                        number,
                        money(nominalStep.cut()),
                        money(nominalStep.priceAfter()));
        };
    }

    static String problem(InputProblem problem) {
        return switch (problem) {
            case NEGATIVE_PRICE -> Messages.get("productivity.calculator.error.negativePrice");
            case DISCOUNT_OUT_OF_RANGE -> Messages.get("productivity.calculator.error.discountRange");
            case TOO_MANY_DISCOUNTS ->
                Messages.format("productivity.calculator.error.tooManyDiscounts", DiscountRequest.MAX_DISCOUNTS);
            case NEGATIVE_DEDUCTION -> Messages.get("productivity.calculator.error.negativeDeduction");
            case PRINCIPAL_NOT_POSITIVE -> Messages.get("productivity.calculator.error.principal");
            case TENOR_OUT_OF_RANGE ->
                Messages.format("productivity.calculator.error.tenor", InstallmentRequest.MAX_TENOR_MONTHS);
            case RATE_OUT_OF_RANGE ->
                Messages.format(
                        "productivity.calculator.error.rate",
                        RupiahFormat.plain(InstallmentRequest.MAX_ANNUAL_RATE_PERCENT, 0));
            case NEGATIVE_FEE -> Messages.get("productivity.calculator.error.negativeFee");
            case FEE_PERCENT_OUT_OF_RANGE -> Messages.get("productivity.calculator.error.feePercent");
        };
    }

    static void showError(Window owner, Throwable error) {
        if (error instanceof ScheduleExportException exportError) {
            ErrorDialogs.show(
                    owner,
                    Messages.get("productivity.calculator.error.export.header"),
                    Messages.format("productivity.calculator.error.export", exportError.fileName()));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }
}
