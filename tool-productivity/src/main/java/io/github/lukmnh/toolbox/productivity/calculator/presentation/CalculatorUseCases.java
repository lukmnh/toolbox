package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.productivity.calculator.application.CalculateDiscountUseCase;
import io.github.lukmnh.toolbox.productivity.calculator.application.ExportScheduleUseCase;
import io.github.lukmnh.toolbox.productivity.calculator.application.SimulateInstallmentUseCase;
import java.util.Objects;

/** Kumpulan use case Kalkulator Cepat, dirakit di composition root. */
public record CalculatorUseCases(
        CalculateDiscountUseCase discount, SimulateInstallmentUseCase installment, ExportScheduleUseCase export) {

    public CalculatorUseCases {
        Objects.requireNonNull(discount, "discount");
        Objects.requireNonNull(installment, "installment");
        Objects.requireNonNull(export, "export");
    }
}
