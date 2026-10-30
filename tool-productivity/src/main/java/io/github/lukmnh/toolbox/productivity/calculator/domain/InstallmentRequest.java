package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Input Simulasi Cicilan.
 *
 * @param annualRatePercent bunga per tahun dalam persen; 0 diperbolehkan (cicilan tanpa bunga)
 * @param moneyScale jumlah desimal pembulatan per bulan (0 = rupiah bulat)
 */
public record InstallmentRequest(
        BigDecimal principal,
        int tenorMonths,
        BigDecimal annualRatePercent,
        InstallmentMethod method,
        AdminFee fee,
        int moneyScale) {

    /** 50 tahun: di atas tenor KPR terpanjang, sekaligus membatasi ukuran tabel. */
    public static final int MAX_TENOR_MONTHS = 600;

    /** Batas atas longgar supaya pinjaman berbunga sangat tinggi tetap bisa disimulasikan. */
    public static final BigDecimal MAX_ANNUAL_RATE_PERCENT = BigDecimal.valueOf(1_000);

    public InstallmentRequest {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(annualRatePercent, "annualRatePercent");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(fee, "fee");
        Money.requireScale(moneyScale);
        if (principal.signum() <= 0) {
            throw new CalculatorInputException(InputProblem.PRINCIPAL_NOT_POSITIVE);
        }
        if (tenorMonths < 1 || tenorMonths > MAX_TENOR_MONTHS) {
            throw new CalculatorInputException(InputProblem.TENOR_OUT_OF_RANGE);
        }
        if (annualRatePercent.signum() < 0 || annualRatePercent.compareTo(MAX_ANNUAL_RATE_PERCENT) > 0) {
            throw new CalculatorInputException(InputProblem.RATE_OUT_OF_RANGE);
        }
    }
}
