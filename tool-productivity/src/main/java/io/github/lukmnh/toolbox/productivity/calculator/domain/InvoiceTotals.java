package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Total invoice. {@code subtotal − discount = tax.base()} dan {@code grandTotal = tax.total()}.
 *
 * @param lineAmounts nominal per baris, urutan sama dengan {@link InvoicePricing#lines()}
 */
public record InvoiceTotals(
        List<BigDecimal> lineAmounts,
        BigDecimal subtotal,
        BigDecimal discountPercent,
        BigDecimal discount,
        TaxResult tax) {

    public InvoiceTotals {
        lineAmounts = List.copyOf(lineAmounts);
        Objects.requireNonNull(subtotal, "subtotal");
        Objects.requireNonNull(discountPercent, "discountPercent");
        Objects.requireNonNull(discount, "discount");
        Objects.requireNonNull(tax, "tax");
    }

    public BigDecimal taxBase() {
        return tax.base();
    }

    public BigDecimal grandTotal() {
        return tax.total();
    }
}
