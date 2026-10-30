package io.github.lukmnh.toolbox.productivity.calculator.domain;

/** Alasan input kalkulator ditolak; presentation memetakannya ke pesan. */
public enum InputProblem {
    NEGATIVE_PRICE,
    DISCOUNT_OUT_OF_RANGE,
    TOO_MANY_DISCOUNTS,
    NEGATIVE_DEDUCTION,
    PRINCIPAL_NOT_POSITIVE,
    TENOR_OUT_OF_RANGE,
    RATE_OUT_OF_RANGE,
    NEGATIVE_FEE,
    FEE_PERCENT_OUT_OF_RANGE
}
