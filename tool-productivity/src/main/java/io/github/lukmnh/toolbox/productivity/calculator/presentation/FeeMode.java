package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.productivity.calculator.domain.AdminFee;
import java.math.BigDecimal;

/** Cara user mengisi biaya admin/provisi. */
enum FeeMode {
    NOMINAL,
    PERCENT;

    AdminFee toFee(BigDecimal value) {
        return switch (this) {
            case NOMINAL -> new AdminFee.Nominal(value);
            case PERCENT -> new AdminFee.Percent(value);
        };
    }
}
