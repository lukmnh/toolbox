package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Bagian invoice yang menentukan angka: baris, diskon, dan pajak. Dipisah dari {@link Invoice} supaya total bisa
 * dihitung langsung saat form belum lengkap (nomor/penjual belum diisi).
 *
 * @param discountPercent diskon atas subtotal (0–100)
 * @param taxPercent tarif pajak atas subtotal setelah diskon (0–100)
 */
public record InvoicePricing(
        List<InvoiceLine> lines, BigDecimal discountPercent, BigDecimal taxPercent, int moneyScale) {

    public static final int MAX_LINES = 200;

    public InvoicePricing {
        lines = List.copyOf(lines);
        Objects.requireNonNull(discountPercent, "discountPercent");
        Objects.requireNonNull(taxPercent, "taxPercent");
        Money.requireScale(moneyScale);
        if (lines.size() > MAX_LINES) {
            throw new CalculatorInputException(InputProblem.TOO_MANY_LINES);
        }
        if (!Money.isPercent(discountPercent)) {
            throw new CalculatorInputException(InputProblem.DISCOUNT_OUT_OF_RANGE);
        }
        TaxCalculator.requireRate(taxPercent);
    }
}
