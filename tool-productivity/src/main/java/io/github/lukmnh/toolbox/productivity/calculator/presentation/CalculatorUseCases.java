package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.productivity.calculator.application.CalculateDiscountUseCase;
import io.github.lukmnh.toolbox.productivity.calculator.application.CalculateInvoiceUseCase;
import io.github.lukmnh.toolbox.productivity.calculator.application.CalculateTaxUseCase;
import io.github.lukmnh.toolbox.productivity.calculator.application.ExportPdfUseCase;
import io.github.lukmnh.toolbox.productivity.calculator.application.ExportScheduleUseCase;
import io.github.lukmnh.toolbox.productivity.calculator.application.SimulateInstallmentUseCase;
import java.util.Objects;

/**
 * Kumpulan use case Installment Simulator, dirakit di composition root.
 *
 * @param export export jadwal cicilan ke CSV
 * @param exportPdf export simulasi cicilan dan invoice ke PDF
 */
public record CalculatorUseCases(
        CalculateDiscountUseCase discount,
        SimulateInstallmentUseCase installment,
        ExportScheduleUseCase export,
        ExportPdfUseCase exportPdf,
        CalculateTaxUseCase tax,
        CalculateInvoiceUseCase invoice) {

    public CalculatorUseCases {
        Objects.requireNonNull(discount, "discount");
        Objects.requireNonNull(installment, "installment");
        Objects.requireNonNull(export, "export");
        Objects.requireNonNull(exportPdf, "exportPdf");
        Objects.requireNonNull(tax, "tax");
        Objects.requireNonNull(invoice, "invoice");
    }
}
