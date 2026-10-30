package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Biaya admin/provisi di awal pinjaman: nominal tetap atau persen dari pokok. */
public sealed interface AdminFee {

    AdminFee NONE = new Nominal(BigDecimal.ZERO);

    /** @return biaya dalam uang, dibulatkan HALF_UP ke {@code scale} */
    BigDecimal amountFor(BigDecimal principal, int scale);

    record Nominal(BigDecimal amount) implements AdminFee {

        public Nominal {
            Objects.requireNonNull(amount, "amount");
            if (amount.signum() < 0) {
                throw new CalculatorInputException(InputProblem.NEGATIVE_FEE);
            }
        }

        @Override
        public BigDecimal amountFor(BigDecimal principal, int scale) {
            return Money.round(amount, scale);
        }
    }

    record Percent(BigDecimal percent) implements AdminFee {

        public Percent {
            Objects.requireNonNull(percent, "percent");
            if (!Money.isPercent(percent)) {
                throw new CalculatorInputException(InputProblem.FEE_PERCENT_OUT_OF_RANGE);
            }
        }

        @Override
        public BigDecimal amountFor(BigDecimal principal, int scale) {
            return Money.round(principal.multiply(percent.movePointLeft(2)), scale);
        }
    }
}
