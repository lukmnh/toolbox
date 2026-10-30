package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Jadwal cicilan Anuitas / Flat / Efektif.
 *
 * <p>Tiap bulan dibulatkan HALF_UP ke skala uang (rupiah). Selisih akibat pembulatan dikoreksi di bulan terakhir:
 * pokok bulan terakhir = sisa pokok, sehingga sisa pokok akhir selalu tepat 0.
 */
public final class InstallmentCalculator {

    /** 34 digit: cukup untuk {@code (1 + r)^600} tanpa galat yang terlihat setelah dibulatkan ke rupiah. */
    private static final MathContext PRECISION = MathContext.DECIMAL128;

    /** Bunga % per tahun → desimal per bulan: dibagi 100 lalu 12. */
    private static final BigDecimal PERCENT_PER_YEAR_TO_MONTHLY = BigDecimal.valueOf(1_200);

    private InstallmentCalculator() {}

    public static InstallmentSchedule calculate(InstallmentRequest request) {
        int scale = request.moneyScale();
        BigDecimal principal = Money.round(request.principal(), scale);
        BigDecimal monthlyRate = request.annualRatePercent().divide(PERCENT_PER_YEAR_TO_MONTHLY, PRECISION);
        int months = request.tenorMonths();
        MonthRule rule = switch (request.method()) {
            case ANNUITY -> annuity(principal, months, monthlyRate, scale);
            case FLAT -> flat(principal, months, monthlyRate, scale);
            case EFFECTIVE -> effective(principal, months, monthlyRate, scale);
        };
        List<AmortizationRow> rows = schedule(principal, months, rule);
        BigDecimal fee = request.fee().amountFor(principal, scale);
        return InstallmentSchedule.of(request.method(), principal, rows, fee);
    }

    /** Cicilan tetap {@code P·r / (1 − (1 + r)^−n)}; bunga 0% → {@code P / n}. */
    private static MonthRule annuity(BigDecimal principal, int months, BigDecimal rate, int scale) {
        BigDecimal payment = Money.round(annuityPayment(principal, months, rate), scale);
        return (month, remaining) -> {
            BigDecimal interest = Money.round(remaining.multiply(rate), scale);
            return new Split(payment.subtract(interest).max(BigDecimal.ZERO), interest);
        };
    }

    static BigDecimal annuityPayment(BigDecimal principal, int months, BigDecimal rate) {
        if (rate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), PRECISION);
        }
        BigDecimal growth = BigDecimal.ONE.add(rate).pow(months, PRECISION);
        return principal.multiply(rate).multiply(growth).divide(growth.subtract(BigDecimal.ONE), PRECISION);
    }

    /** Total bunga flat dihitung sekali lalu dibagi rata; sisa pembulatan masuk bulan terakhir. */
    private static MonthRule flat(BigDecimal principal, int months, BigDecimal rate, int scale) {
        BigDecimal principalPart = equalPrincipal(principal, months, scale);
        BigDecimal totalInterest = Money.round(principal.multiply(rate).multiply(BigDecimal.valueOf(months)), scale);
        List<BigDecimal> interests = spread(totalInterest, months, scale);
        return (month, remaining) -> new Split(principalPart, interests.get(month - 1));
    }

    private static MonthRule effective(BigDecimal principal, int months, BigDecimal rate, int scale) {
        BigDecimal principalPart = equalPrincipal(principal, months, scale);
        return (month, remaining) -> new Split(principalPart, Money.round(remaining.multiply(rate), scale));
    }

    private static BigDecimal equalPrincipal(BigDecimal principal, int months, int scale) {
        return Money.round(principal.divide(BigDecimal.valueOf(months), PRECISION), scale);
    }

    /** Bagi {@code total} ke {@code months} porsi; tidak pernah negatif walau pembulatan per porsi naik. */
    private static List<BigDecimal> spread(BigDecimal total, int months, int scale) {
        BigDecimal portion = Money.round(total.divide(BigDecimal.valueOf(months), PRECISION), scale);
        List<BigDecimal> portions = new ArrayList<>(months);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int month = 1; month < months; month++) {
            BigDecimal value = portion.min(total.subtract(allocated));
            portions.add(value);
            allocated = allocated.add(value);
        }
        portions.add(total.subtract(allocated));
        return portions;
    }

    private static List<AmortizationRow> schedule(BigDecimal principal, int months, MonthRule rule) {
        List<AmortizationRow> rows = new ArrayList<>(months);
        BigDecimal remaining = principal;
        for (int month = 1; month <= months; month++) {
            Split split = rule.split(month, remaining);
            // Bulan terakhir melunasi seluruh sisa (koreksi pembulatan); bulan lain tidak boleh melebihi sisa
            BigDecimal principalPart =
                    month == months ? remaining : split.principal().min(remaining);
            remaining = remaining.subtract(principalPart);
            rows.add(new AmortizationRow(
                    month, principalPart.add(split.interest()), principalPart, split.interest(), remaining));
        }
        return rows;
    }

    @FunctionalInterface
    private interface MonthRule {

        /** @param remaining sisa pokok sebelum bulan ini */
        Split split(int month, BigDecimal remaining);
    }

    private record Split(BigDecimal principal, BigDecimal interest) {}
}
