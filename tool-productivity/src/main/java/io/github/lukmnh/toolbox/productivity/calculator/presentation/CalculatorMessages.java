package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.productivity.calculator.domain.DiscountRequest;
import io.github.lukmnh.toolbox.productivity.calculator.domain.DiscountStep;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InputProblem;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentMethod;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentRequest;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceLine;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoicePricing;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceType;
import io.github.lukmnh.toolbox.productivity.calculator.domain.PdfExportException;
import io.github.lukmnh.toolbox.productivity.calculator.domain.ScheduleExportException;
import io.github.lukmnh.toolbox.productivity.calculator.domain.TaxMode;
import io.github.lukmnh.toolbox.productivity.shared.domain.RupiahFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import javafx.stage.Window;

/** Format uang/persen/tanggal dan pemetaan error Installment Simulator ke teks lewat {@link Messages}. */
final class CalculatorMessages {

    /**
     * Settings belum punya opsi 0/2 desimal, jadi sementara Rupiah bulat. Dipakai juga sebagai skala pembulatan
     * hitungan supaya angka yang tampil = angka yang dihitung.
     */
    static final int MONEY_SCALE = RupiahFormat.NO_DECIMALS;

    private static final int PERCENT_FRACTION_DIGITS = 2;
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Messages.LOCALE);

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

    static String taxMode(TaxMode mode) {
        return Messages.get("productivity.calculator.tax.mode." + mode.name().toLowerCase(Locale.ROOT));
    }

    static String invoiceType(InvoiceType type) {
        return Messages.get(
                "productivity.calculator.invoice.type." + type.name().toLowerCase(Locale.ROOT));
    }

    static String date(LocalDate date) {
        return DATE.format(date);
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
            case NEGATIVE_AMOUNT -> Messages.get("productivity.calculator.error.negativeAmount");
            case TAX_RATE_OUT_OF_RANGE -> Messages.get("productivity.calculator.error.taxRate");
            case NO_LINES -> Messages.get("productivity.calculator.error.noLines");
            case TOO_MANY_LINES ->
                Messages.format("productivity.calculator.error.tooManyLines", InvoicePricing.MAX_LINES);
            case LINE_DESCRIPTION_REQUIRED -> Messages.get("productivity.calculator.error.lineDescription");
            case QUANTITY_OUT_OF_RANGE ->
                Messages.format(
                        "productivity.calculator.error.quantity", RupiahFormat.plain(InvoiceLine.MAX_QUANTITY, 0));
            case UNIT_PRICE_OUT_OF_RANGE ->
                Messages.format("productivity.calculator.error.unitPrice", money(InvoiceLine.MAX_UNIT_PRICE));
            case DOCUMENT_NUMBER_REQUIRED -> Messages.get("productivity.calculator.error.documentNumber");
            case SELLER_REQUIRED -> Messages.get("productivity.calculator.error.seller");
            case TEXT_TOO_LONG -> Messages.get("productivity.calculator.error.textTooLong");
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
        if (error instanceof PdfExportException pdfError) {
            ErrorDialogs.show(
                    owner,
                    Messages.get("productivity.calculator.error.pdf.header"),
                    Messages.format("productivity.calculator.error.pdf", pdfError.fileName()));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }
}
