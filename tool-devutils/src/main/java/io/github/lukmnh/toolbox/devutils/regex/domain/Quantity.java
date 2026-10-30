package io.github.lukmnh.toolbox.devutils.regex.domain;

/** Pengulangan yang menempel pada satu baris penjelasan. */
public record Quantity(int min, int max, QuantifierMode mode) {

    /** Tanpa quantifier (tepat sekali). */
    public static final Quantity ONCE = new Quantity(1, 1, QuantifierMode.GREEDY);

    public boolean isOnce() {
        return equals(ONCE);
    }
}
