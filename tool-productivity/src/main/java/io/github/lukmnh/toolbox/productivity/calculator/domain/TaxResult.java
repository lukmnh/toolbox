package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Hasil hitung pajak. Selalu {@code base + tax = total}.
 *
 * @param base DPP (dasar pengenaan pajak)
 */
public record TaxResult(BigDecimal base, BigDecimal ratePercent, BigDecimal tax, BigDecimal total) {

    public TaxResult {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(ratePercent, "ratePercent");
        Objects.requireNonNull(tax, "tax");
        Objects.requireNonNull(total, "total");
        if (base.add(tax).compareTo(total) != 0) {
            throw new IllegalArgumentException("Tax base + tax must equal the total");
        }
    }
}
