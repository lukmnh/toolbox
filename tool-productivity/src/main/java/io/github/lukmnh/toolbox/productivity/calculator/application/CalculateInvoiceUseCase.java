package io.github.lukmnh.toolbox.productivity.calculator.application;

import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceCalculator;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoicePricing;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceTotals;

/** Hitung subtotal, diskon, DPP, pajak, dan grand total invoice. Maks 200 baris, cukup di FX thread. */
public final class CalculateInvoiceUseCase {

    public InvoiceTotals execute(InvoicePricing pricing) {
        return InvoiceCalculator.calculate(pricing);
    }
}
