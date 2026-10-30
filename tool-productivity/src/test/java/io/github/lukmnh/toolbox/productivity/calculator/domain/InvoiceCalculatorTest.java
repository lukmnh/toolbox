package io.github.lukmnh.toolbox.productivity.calculator.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class InvoiceCalculatorTest {

    private static final int RUPIAH = 0;
    private static final InvoiceLine CONSULTING = line("Consulting", "2", "50000");
    private static final InvoiceLine MATERIAL = line("Material", "1.5", "10000");

    @Test
    void appliesDiscountBeforeTax() {
        InvoiceTotals totals = InvoiceCalculator.calculate(pricing(List.of(CONSULTING, MATERIAL), "10", "12"));

        assertMoney("100000", totals.lineAmounts().get(0));
        assertMoney("15000", totals.lineAmounts().get(1));
        assertMoney("115000", totals.subtotal());
        assertMoney("11500", totals.discount());
        assertMoney("103500", totals.taxBase());
        assertMoney("12420", totals.tax().tax());
        assertMoney("115920", totals.grandTotal());
    }

    @Test
    void roundsEachLineAndStep() {
        // 3 × 333,33 = 999,99 → 1.000; diskon 5% = 50; DPP 950; PPN 11% = 104,5 → 105
        InvoiceTotals totals = InvoiceCalculator.calculate(pricing(List.of(line("Pen", "3", "333.33")), "5", "11"));

        assertMoney("1000", totals.subtotal());
        assertMoney("50", totals.discount());
        assertMoney("950", totals.taxBase());
        assertMoney("105", totals.tax().tax());
        assertMoney("1055", totals.grandTotal());
    }

    @Test
    void emptyPricingIsZero() {
        InvoiceTotals totals = InvoiceCalculator.calculate(pricing(List.of(), "0", "12"));

        assertMoney("0", totals.grandTotal());
    }

    @Test
    void enforcesLineLimitsAndRanges() {
        assertProblem(
                InputProblem.TOO_MANY_LINES,
                () -> pricing(Collections.nCopies(InvoicePricing.MAX_LINES + 1, CONSULTING), "0", "12"));
        assertProblem(InputProblem.DISCOUNT_OUT_OF_RANGE, () -> pricing(List.of(CONSULTING), "101", "12"));
        assertProblem(InputProblem.TAX_RATE_OUT_OF_RANGE, () -> pricing(List.of(CONSULTING), "0", "-1"));
        assertProblem(InputProblem.LINE_DESCRIPTION_REQUIRED, () -> line("  ", "1", "1"));
        assertProblem(InputProblem.QUANTITY_OUT_OF_RANGE, () -> line("A", "0", "1"));
        assertProblem(InputProblem.QUANTITY_OUT_OF_RANGE, () -> line("A", "1000001", "1"));
        assertProblem(InputProblem.UNIT_PRICE_OUT_OF_RANGE, () -> line("A", "1", "-1"));
        assertProblem(
                InputProblem.TEXT_TOO_LONG, () -> line("x".repeat(InvoiceLine.MAX_DESCRIPTION_LENGTH + 1), "1", "1"));
    }

    @Test
    void maxLinesIsAccepted() {
        InvoiceTotals totals = InvoiceCalculator.calculate(
                pricing(Collections.nCopies(InvoicePricing.MAX_LINES, line("A", "1", "1")), "0", "0"));

        assertMoney(String.valueOf(InvoicePricing.MAX_LINES), totals.grandTotal());
    }

    @Test
    void completeInvoiceRequiresNumberSellerAndLines() {
        InvoicePricing withLines = pricing(List.of(CONSULTING), "0", "12");
        InvoiceParty seller = new InvoiceParty("Toko Maju", "Jakarta");
        InvoiceParty nobody = new InvoiceParty("", "");

        assertProblem(InputProblem.DOCUMENT_NUMBER_REQUIRED, () -> invoice(" ", seller, withLines, ""));
        assertProblem(InputProblem.SELLER_REQUIRED, () -> invoice("INV-1", nobody, withLines, ""));
        assertProblem(InputProblem.NO_LINES, () -> invoice("INV-1", seller, pricing(List.of(), "0", "12"), ""));
        assertProblem(
                InputProblem.TEXT_TOO_LONG,
                () -> invoice("INV-1", seller, withLines, "n".repeat(Invoice.MAX_NOTES_LENGTH + 1)));

        Invoice valid = invoice("  INV-1  ", seller, withLines, "  Thanks  ");
        assertEquals("INV-1", valid.number());
        assertEquals("Thanks", valid.notes());
    }

    private static Invoice invoice(String number, InvoiceParty seller, InvoicePricing pricing, String notes) {
        return new Invoice(
                InvoiceType.INVOICE,
                number,
                LocalDate.of(2026, 1, 31),
                seller,
                new InvoiceParty("", ""),
                pricing,
                notes);
    }

    private static InvoiceLine line(String description, String quantity, String unitPrice) {
        return new InvoiceLine(description, new BigDecimal(quantity), new BigDecimal(unitPrice));
    }

    private static InvoicePricing pricing(List<InvoiceLine> lines, String discount, String tax) {
        return new InvoicePricing(lines, new BigDecimal(discount), new BigDecimal(tax), RUPIAH);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> expected + " != " + actual);
    }

    private static void assertProblem(InputProblem expected, Executable action) {
        assertEquals(
                expected, assertThrows(CalculatorInputException.class, action).problem());
    }
}
