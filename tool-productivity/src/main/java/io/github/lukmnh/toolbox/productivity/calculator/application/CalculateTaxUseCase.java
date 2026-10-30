package io.github.lukmnh.toolbox.productivity.calculator.application;

import io.github.lukmnh.toolbox.productivity.calculator.domain.TaxCalculator;
import io.github.lukmnh.toolbox.productivity.calculator.domain.TaxRequest;
import io.github.lukmnh.toolbox.productivity.calculator.domain.TaxResult;

/** Hitung DPP, pajak, dan total dari harga sebelum atau sesudah pajak. */
public final class CalculateTaxUseCase {

    public TaxResult execute(TaxRequest request) {
        return TaxCalculator.calculate(request);
    }
}
