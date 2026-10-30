package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ringkasan budget: pemasukan vs pengeluaran per bulan.
 *
 * @param totalIncome total pemasukan per bulan
 * @param totalMonthly total pengeluaran per bulan (semua daftar, item aktif)
 */
public record BudgetSummary(BigDecimal totalIncome, List<SheetTotals> sheets, BigDecimal totalMonthly) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int PERCENT_SCALE = 4;

    public BudgetSummary {
        Objects.requireNonNull(totalIncome, "totalIncome");
        Objects.requireNonNull(totalMonthly, "totalMonthly");
        sheets = List.copyOf(sheets);
    }

    /** Negatif berarti pengeluaran melebihi pemasukan. */
    public BigDecimal remaining() {
        return totalIncome.subtract(totalMonthly);
    }

    public BigDecimal perYear() {
        return MonthlyCost.perYear(totalMonthly);
    }

    /** @return kosong bila belum ada pemasukan (rasio tidak terdefinisi) */
    public Optional<BigDecimal> expenseRatioPercent() {
        if (totalIncome.signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(totalMonthly.multiply(HUNDRED).divide(totalIncome, PERCENT_SCALE, RoundingMode.HALF_UP));
    }
}
