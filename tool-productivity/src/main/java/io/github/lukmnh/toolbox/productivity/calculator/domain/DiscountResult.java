package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Hasil Diskon Ganda.
 *
 * @param effectiveDiscountPercent {@code (1 − Π(1 − dᵢ)) × 100}, hanya dari diskon persen
 * @param clamped {@code true} bila potongan nominal melebihi harga sehingga harga akhir dipaksa 0
 */
public record DiscountResult(
        BigDecimal originalPrice,
        BigDecimal finalPrice,
        BigDecimal totalSaving,
        BigDecimal effectiveDiscountPercent,
        List<DiscountStep> steps,
        boolean clamped) {

    public DiscountResult {
        Objects.requireNonNull(originalPrice, "originalPrice");
        Objects.requireNonNull(finalPrice, "finalPrice");
        Objects.requireNonNull(totalSaving, "totalSaving");
        Objects.requireNonNull(effectiveDiscountPercent, "effectiveDiscountPercent");
        steps = List.copyOf(steps);
        if (finalPrice.signum() < 0) {
            throw new IllegalArgumentException("Harga akhir tidak boleh negatif");
        }
    }
}
