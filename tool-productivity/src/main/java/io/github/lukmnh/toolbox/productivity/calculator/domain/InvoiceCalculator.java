package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Subtotal → diskon → DPP → pajak → total. Tiap baris dan tiap langkah dibulatkan HALF_UP ke skala uang, sehingga
 * angka yang tercetak selalu bisa dijumlahkan ulang secara manual.
 */
public final class InvoiceCalculator {

    private InvoiceCalculator() {}

    public static InvoiceTotals calculate(InvoicePricing pricing) {
        int scale = pricing.moneyScale();
        List<BigDecimal> amounts =
                pricing.lines().stream().map(line -> line.amount(scale)).toList();
        BigDecimal subtotal = Money.round(amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add), scale);
        BigDecimal discount =
                Money.round(subtotal.multiply(pricing.discountPercent().movePointLeft(2)), scale);
        TaxResult tax = TaxCalculator.fromBase(subtotal.subtract(discount), pricing.taxPercent(), scale);
        return new InvoiceTotals(amounts, subtotal, pricing.discountPercent(), discount, tax);
    }
}
