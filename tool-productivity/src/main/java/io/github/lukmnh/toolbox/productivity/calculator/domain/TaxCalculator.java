package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Pajak/PPN eksklusif (dari DPP) dan inklusif (dari harga termasuk pajak), dibulatkan HALF_UP ke skala uang.
 *
 * <p>Mode inklusif membulatkan DPP lalu {@code pajak = total − DPP}, jadi DPP + pajak selalu tepat sama dengan harga
 * yang diketik. Bolak-balik eksklusif → inklusif mengembalikan DPP semula karena galat pembulatan pajak (≤ 0,5)
 * dibagi {@code 1 + tarif} tetap di bawah 0,5.
 */
public final class TaxCalculator {

    private static final MathContext PRECISION = MathContext.DECIMAL128;

    private TaxCalculator() {}

    public static TaxResult calculate(TaxRequest request) {
        return switch (request.mode()) {
            case EXCLUSIVE -> fromBase(request.amount(), request.ratePercent(), request.moneyScale());
            case INCLUSIVE -> fromTotal(request.amount(), request.ratePercent(), request.moneyScale());
        };
    }

    /** Pajak di atas DPP; dipakai juga oleh invoice. */
    static TaxResult fromBase(BigDecimal amount, BigDecimal ratePercent, int scale) {
        BigDecimal base = Money.round(amount, scale);
        BigDecimal tax = Money.round(base.multiply(ratePercent.movePointLeft(2)), scale);
        return new TaxResult(base, ratePercent, tax, base.add(tax));
    }

    private static TaxResult fromTotal(BigDecimal amount, BigDecimal ratePercent, int scale) {
        BigDecimal total = Money.round(amount, scale);
        BigDecimal divisor = BigDecimal.ONE.add(ratePercent.movePointLeft(2));
        BigDecimal base = Money.round(total.divide(divisor, PRECISION), scale);
        return new TaxResult(base, ratePercent, total.subtract(base), total);
    }

    static void requireRate(BigDecimal ratePercent) {
        if (!Money.isPercent(ratePercent)) {
            throw new CalculatorInputException(InputProblem.TAX_RATE_OUT_OF_RANGE);
        }
    }
}
