package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Input Diskon Ganda.
 *
 * @param discountPercents diskon bertingkat dalam persen (0–100), diterapkan berurutan
 * @param deduction potongan nominal setelah semua diskon persen; {@code 0} bila tidak ada
 * @param moneyScale jumlah desimal uang hasil (0 = rupiah bulat)
 */
public record DiscountRequest(
        BigDecimal originalPrice, List<BigDecimal> discountPercents, BigDecimal deduction, int moneyScale) {

    public static final int MAX_DISCOUNTS = 20;

    public DiscountRequest {
        Objects.requireNonNull(originalPrice, "originalPrice");
        Objects.requireNonNull(deduction, "deduction");
        discountPercents = List.copyOf(discountPercents);
        Money.requireScale(moneyScale);
        if (originalPrice.signum() < 0) {
            throw new CalculatorInputException(InputProblem.NEGATIVE_PRICE);
        }
        if (discountPercents.size() > MAX_DISCOUNTS) {
            throw new CalculatorInputException(InputProblem.TOO_MANY_DISCOUNTS);
        }
        if (!discountPercents.stream().allMatch(Money::isPercent)) {
            throw new CalculatorInputException(InputProblem.DISCOUNT_OUT_OF_RANGE);
        }
        if (deduction.signum() < 0) {
            throw new CalculatorInputException(InputProblem.NEGATIVE_DEDUCTION);
        }
    }
}
