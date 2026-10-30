package io.github.lukmnh.toolbox.productivity.calculator.domain;

import java.math.BigDecimal;

/** Satu tahap potongan harga. {@code cut} = nominal yang benar-benar dipotong di tahap ini. */
public sealed interface DiscountStep {

    BigDecimal priceBefore();

    BigDecimal cut();

    BigDecimal priceAfter();

    record Percent(BigDecimal percent, BigDecimal priceBefore, BigDecimal cut, BigDecimal priceAfter)
            implements DiscountStep {}

    /** @param requested potongan yang diminta; bisa lebih besar dari {@code cut} bila harga sudah habis */
    record Nominal(BigDecimal requested, BigDecimal priceBefore, BigDecimal cut, BigDecimal priceAfter)
            implements DiscountStep {}
}
