package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Input Kalkulator Pajak/PPN.
 *
 * @param ratePercent tarif pajak dalam persen (0–100)
 * @param moneyScale jumlah desimal uang hasil (0 = rupiah bulat)
 */
public record TaxRequest(BigDecimal amount, BigDecimal ratePercent, TaxMode mode, int moneyScale) {

    /** Tarif PPN umum yang berlaku saat ini. */
    public static final BigDecimal DEFAULT_RATE_PERCENT = BigDecimal.valueOf(12);

    public TaxRequest {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(ratePercent, "ratePercent");
        Objects.requireNonNull(mode, "mode");
        Money.requireScale(moneyScale);
        if (amount.signum() < 0) {
            throw new CalculatorInputException(InputProblem.NEGATIVE_AMOUNT);
        }
        TaxCalculator.requireRate(ratePercent);
    }
}
