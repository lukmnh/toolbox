package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Hasil Simulasi Cicilan.
 *
 * @param totalInstallments jumlah semua cicilan (pokok + bunga)
 * @param totalCost {@code totalInstallments + fee}
 */
public record InstallmentSchedule(
        InstallmentMethod method,
        BigDecimal principal,
        List<AmortizationRow> rows,
        BigDecimal totalInterest,
        BigDecimal totalInstallments,
        BigDecimal fee,
        BigDecimal totalCost) {

    public InstallmentSchedule {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(totalInterest, "totalInterest");
        Objects.requireNonNull(totalInstallments, "totalInstallments");
        Objects.requireNonNull(fee, "fee");
        Objects.requireNonNull(totalCost, "totalCost");
        rows = List.copyOf(rows);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Jadwal minimal 1 bulan");
        }
    }

    static InstallmentSchedule of(
            InstallmentMethod method, BigDecimal principal, List<AmortizationRow> rows, BigDecimal fee) {
        BigDecimal interest = rows.stream().map(AmortizationRow::interest).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal installments = rows.stream().map(AmortizationRow::payment).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new InstallmentSchedule(method, principal, rows, interest, installments, fee, installments.add(fee));
    }

    public BigDecimal firstPayment() {
        return rows.getFirst().payment();
    }

    public BigDecimal lastPayment() {
        return rows.getLast().payment();
    }

    public int tenorMonths() {
        return rows.size();
    }
}
