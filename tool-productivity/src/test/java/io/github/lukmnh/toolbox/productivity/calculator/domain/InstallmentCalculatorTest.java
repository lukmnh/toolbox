package io.github.lukmnh.toolbox.productivity.calculator.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Rp12 juta, 12% per tahun (1% per bulan), 12 bulan: angka mudah dicek manual untuk ketiga metode. */
class InstallmentCalculatorTest {

    private static final BigDecimal PRINCIPAL = BigDecimal.valueOf(12_000_000);
    private static final BigDecimal RATE = BigDecimal.valueOf(12);
    private static final int MONTHS = 12;

    @Test
    void flatChargesInterestOnOriginalPrincipal() {
        InstallmentSchedule schedule = simulate(InstallmentMethod.FLAT, AdminFee.NONE);

        for (AmortizationRow row : schedule.rows()) {
            assertMoney("1000000", row.principalPart());
            assertMoney("120000", row.interest());
            assertMoney("1120000", row.payment());
        }
        assertMoney("1440000", schedule.totalInterest());
        assertMoney("11000000", schedule.rows().getFirst().remaining());
    }

    @Test
    void effectiveChargesInterestOnRemainingPrincipal() {
        InstallmentSchedule schedule = simulate(InstallmentMethod.EFFECTIVE, AdminFee.NONE);

        assertMoney("1120000", schedule.firstPayment());
        assertMoney("1010000", schedule.lastPayment());
        AmortizationRow second = schedule.rows().get(1);
        assertMoney("1000000", second.principalPart());
        assertMoney("110000", second.interest());
        // 1% × 1 juta × (12 + 11 + … + 1)
        assertMoney("780000", schedule.totalInterest());
    }

    @Test
    void annuityKeepsPaymentFixedWhileInterestDeclines() {
        InstallmentSchedule schedule = simulate(InstallmentMethod.ANNUITY, AdminFee.NONE);
        List<AmortizationRow> rows = schedule.rows();

        // 12.000.000 × 0,01 / (1 − 1,01^−12) = 1.066.185,46
        assertMoney("1066185", schedule.firstPayment());
        assertMoney("120000", rows.getFirst().interest());
        assertMoney("946185", rows.getFirst().principalPart());
        for (int index = 1; index < rows.size(); index++) {
            assertTrue(rows.get(index).interest().compareTo(rows.get(index - 1).interest()) < 0);
            if (index < rows.size() - 1) {
                assertMoney("1066185", rows.get(index).payment());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(InstallmentMethod.class)
    void everyMethodPaysOffPrincipalExactly(InstallmentMethod method) {
        InstallmentSchedule schedule = simulate(method, new AdminFee.Percent(BigDecimal.ONE));
        List<AmortizationRow> rows = schedule.rows();

        assertEquals(MONTHS, rows.size());
        assertMoney("0", rows.getLast().remaining());
        BigDecimal principalPaid =
                rows.stream().map(AmortizationRow::principalPart).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertMoney(PRINCIPAL.toPlainString(), principalPaid);
        for (AmortizationRow row : rows) {
            assertEquals(0, row.payment().compareTo(row.principalPart().add(row.interest())));
        }
        assertMoney("120000", schedule.fee());
        assertEquals(
                0,
                schedule.totalCost()
                        .compareTo(PRINCIPAL.add(schedule.totalInterest()).add(schedule.fee())));
    }

    @Test
    void zeroRateSplitsPrincipalEvenly() {
        InstallmentSchedule schedule = InstallmentCalculator.calculate(new InstallmentRequest(
                PRINCIPAL, MONTHS, BigDecimal.ZERO, InstallmentMethod.ANNUITY, AdminFee.NONE, 0));

        assertMoney("1000000", schedule.firstPayment());
        assertMoney("0", schedule.totalInterest());
    }

    private static InstallmentSchedule simulate(InstallmentMethod method, AdminFee fee) {
        return InstallmentCalculator.calculate(new InstallmentRequest(PRINCIPAL, MONTHS, RATE, method, fee, 0));
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> expected + " != " + actual);
    }
}
