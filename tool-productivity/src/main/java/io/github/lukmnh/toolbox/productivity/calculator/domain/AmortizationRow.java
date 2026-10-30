package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Satu baris tabel amortisasi. {@code payment = principalPart + interest}. */
public record AmortizationRow(
        int month, BigDecimal payment, BigDecimal principalPart, BigDecimal interest, BigDecimal remaining) {

    public AmortizationRow {
        Objects.requireNonNull(payment, "payment");
        Objects.requireNonNull(principalPart, "principalPart");
        Objects.requireNonNull(interest, "interest");
        Objects.requireNonNull(remaining, "remaining");
        if (month < 1) {
            throw new IllegalArgumentException("Bulan dimulai dari 1");
        }
    }
}
