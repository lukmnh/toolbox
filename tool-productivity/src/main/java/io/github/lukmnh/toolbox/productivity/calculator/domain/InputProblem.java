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
    FEE_PERCENT_OUT_OF_RANGE,
    NEGATIVE_AMOUNT,
    TAX_RATE_OUT_OF_RANGE,
    NO_LINES,
    TOO_MANY_LINES,
    LINE_DESCRIPTION_REQUIRED,
    QUANTITY_OUT_OF_RANGE,
    UNIT_PRICE_OUT_OF_RANGE,
    DOCUMENT_NUMBER_REQUIRED,
    SELLER_REQUIRED,
    TEXT_TOO_LONG
}
