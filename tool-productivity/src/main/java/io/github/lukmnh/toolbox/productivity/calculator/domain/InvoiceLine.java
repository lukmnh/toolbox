package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Satu baris barang/jasa.
 *
 * @param quantity boleh pecahan (misalnya 1,5 kg), harus lebih dari 0
 */
public record InvoiceLine(String description, BigDecimal quantity, BigDecimal unitPrice) {

    public static final int MAX_DESCRIPTION_LENGTH = 200;
    public static final BigDecimal MAX_QUANTITY = BigDecimal.valueOf(1_000_000);
    /** Rp10 triliun per unit: jauh di atas transaksi wajar, sekaligus menjaga total tetap terbaca. */
    public static final BigDecimal MAX_UNIT_PRICE = BigDecimal.valueOf(10_000_000_000_000L);

    public InvoiceLine {
        description = Objects.requireNonNull(description, "description").strip();
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (description.isEmpty()) {
            throw new CalculatorInputException(InputProblem.LINE_DESCRIPTION_REQUIRED);
        }
        Invoice.requireMaxLength(description, MAX_DESCRIPTION_LENGTH);
        if (quantity.signum() <= 0 || quantity.compareTo(MAX_QUANTITY) > 0) {
            throw new CalculatorInputException(InputProblem.QUANTITY_OUT_OF_RANGE);
        }
        if (unitPrice.signum() < 0 || unitPrice.compareTo(MAX_UNIT_PRICE) > 0) {
            throw new CalculatorInputException(InputProblem.UNIT_PRICE_OUT_OF_RANGE);
        }
    }

    /** {@code qty × harga satuan}, dibulatkan HALF_UP per baris seperti nota pada umumnya. */
    public BigDecimal amount(int scale) {
        return Money.round(quantity.multiply(unitPrice), scale);
    }
}
