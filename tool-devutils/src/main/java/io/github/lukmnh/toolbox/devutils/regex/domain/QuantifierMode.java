package io.github.lukmnh.toolbox.devutils.regex.domain;

public enum QuantifierMode {
    /** Rakus: ambil sebanyak mungkin. */
    GREEDY,
    /** Malas ({@code ?}): ambil sesedikit mungkin. */
    LAZY,
    /** Posesif ({@code +}): rakus tanpa backtracking. */
    POSSESSIVE
}
