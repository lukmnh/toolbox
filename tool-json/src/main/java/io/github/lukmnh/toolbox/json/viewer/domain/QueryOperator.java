package io.github.lukmnh.toolbox.json.viewer.domain;

import java.util.Arrays;
import java.util.Optional;

/** Operator perbandingan biner di filter query. */
public enum QueryOperator {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER(">"),
    LESS("<"),
    GREATER_OR_EQUAL(">="),
    LESS_OR_EQUAL("<="),
    CONTAINS("contains"),
    STARTS_WITH("startsWith");

    private final String symbol;

    QueryOperator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    /** Simbol atau kata kunci operator; kata kunci tidak peka huruf besar/kecil. */
    public static Optional<QueryOperator> fromSymbol(String text) {
        return Arrays.stream(values())
                .filter(operator -> operator.symbol.equalsIgnoreCase(text))
                .findFirst();
    }
}
