package io.github.lukmnh.toolbox.productivity.calculator.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TaxCalculatorTest {

    private static final int RUPIAH = 0;

    @Test
    void exclusiveAddsTaxOnTopOfBase() {
        TaxResult result = calculate("1000000", "12", TaxMode.EXCLUSIVE);

        assertMoney("1000000", result.base());
        assertMoney("120000", result.tax());
        assertMoney("1120000", result.total());
    }

    @Test
    void inclusiveExtractsBaseFromTotal() {
        TaxResult result = calculate("1120000", "12", TaxMode.INCLUSIVE);

        assertMoney("1000000", result.base());
        assertMoney("120000", result.tax());
        assertMoney("1120000", result.total());
    }

    @Test
    void roundsHalfUpToRupiah() {
        // 1.005 × 12% = 120,6 → 121
        TaxResult exclusive = calculate("1005", "12", TaxMode.EXCLUSIVE);
        assertMoney("121", exclusive.tax());
        assertMoney("1126", exclusive.total());

        // 1.000 / 1,11 = 900,9009 → 901, pajak = sisanya supaya DPP + pajak tetap 1.000
        TaxResult inclusive = calculate("1000", "11", TaxMode.INCLUSIVE);
        assertMoney("901", inclusive.base());
        assertMoney("99", inclusive.tax());
        assertMoney("1000", inclusive.total());
    }

    @Test
    void zeroRateMeansNoTax() {
        for (TaxMode mode : TaxMode.values()) {
            TaxResult result = calculate("123456", "0", mode);
            assertMoney("123456", result.base());
            assertMoney("0", result.tax());
            assertMoney("123456", result.total());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "7.5", "11", "12", "100"})
    void exclusiveThenInclusiveRoundTripsToOriginalBase(String rate) {
        for (int base = 0; base <= 5_000; base += 7) {
            TaxResult forward = calculate(String.valueOf(base), rate, TaxMode.EXCLUSIVE);
            TaxResult back = calculate(forward.total().toPlainString(), rate, TaxMode.INCLUSIVE);

            assertMoney(String.valueOf(base), back.base());
            assertMoney(forward.tax().toPlainString(), back.tax());
        }
    }

    @Test
    void supportsTwoDecimalScale() {
        TaxResult result = TaxCalculator.calculate(
                new TaxRequest(new BigDecimal("99.99"), new BigDecimal("12"), TaxMode.EXCLUSIVE, 2));

        assertEquals(new BigDecimal("12.00"), result.tax());
        assertEquals(new BigDecimal("111.99"), result.total());
    }

    @Test
    void rejectsNegativeAmountAndRateOutsideRange() {
        assertProblem(InputProblem.NEGATIVE_AMOUNT, "-1", "12");
        assertProblem(InputProblem.TAX_RATE_OUT_OF_RANGE, "100", "-0.5");
        assertProblem(InputProblem.TAX_RATE_OUT_OF_RANGE, "100", "100.01");
    }

    private static TaxResult calculate(String amount, String rate, TaxMode mode) {
        return TaxCalculator.calculate(new TaxRequest(new BigDecimal(amount), new BigDecimal(rate), mode, RUPIAH));
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> expected + " != " + actual);
    }

    private static void assertProblem(InputProblem expected, String amount, String rate) {
        CalculatorInputException error = assertThrows(
                CalculatorInputException.class,
                () -> new TaxRequest(new BigDecimal(amount), new BigDecimal(rate), TaxMode.EXCLUSIVE, RUPIAH));
        assertEquals(expected, error.problem());
    }
}
