package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Diskon bertingkat: tiap diskon persen dihitung dari harga setelah diskon sebelumnya (50% + 20% = 60%, bukan 70%).
 * Potongan per tahap dibulatkan HALF_UP ke skala uang, sama seperti kasir menghitung per tahap.
 */
public final class DiscountCalculator {

    private DiscountCalculator() {}

    public static DiscountResult calculate(DiscountRequest request) {
        int scale = request.moneyScale();
        BigDecimal original = Money.round(request.originalPrice(), scale);
        BigDecimal price = original;
        BigDecimal remainingFraction = BigDecimal.ONE;
        List<DiscountStep> steps = new ArrayList<>();
        for (BigDecimal percent : request.discountPercents()) {
            BigDecimal fraction = percent.movePointLeft(2);
            BigDecimal cut = Money.round(price.multiply(fraction), scale);
            steps.add(new DiscountStep.Percent(percent, price, cut, price.subtract(cut)));
            price = price.subtract(cut);
            remainingFraction = remainingFraction.multiply(BigDecimal.ONE.subtract(fraction));
        }
        boolean clamped = false;
        BigDecimal deduction = Money.round(request.deduction(), scale);
        if (deduction.signum() > 0) {
            // Harga tidak boleh negatif: potongan yang melebihi harga dipotong sampai 0 dan user diberi peringatan
            clamped = deduction.compareTo(price) > 0;
            BigDecimal cut = deduction.min(price);
            steps.add(new DiscountStep.Nominal(deduction, price, cut, price.subtract(cut)));
            price = price.subtract(cut);
        }
        BigDecimal effective = BigDecimal.ONE.subtract(remainingFraction).movePointRight(2);
        return new DiscountResult(original, price, original.subtract(price), effective, steps, clamped);
    }
}
