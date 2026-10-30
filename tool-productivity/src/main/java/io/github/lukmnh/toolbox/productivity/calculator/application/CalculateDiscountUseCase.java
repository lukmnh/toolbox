package io.github.lukmnh.toolbox.productivity.calculator.application;

import io.github.lukmnh.toolbox.productivity.calculator.domain.DiscountCalculator;
import io.github.lukmnh.toolbox.productivity.calculator.domain.DiscountRequest;
import io.github.lukmnh.toolbox.productivity.calculator.domain.DiscountResult;

/** Hitung harga akhir, total hemat, dan diskon efektif dari diskon bertingkat. */
public final class CalculateDiscountUseCase {

    public DiscountResult execute(DiscountRequest request) {
        return DiscountCalculator.calculate(request);
    }
}
