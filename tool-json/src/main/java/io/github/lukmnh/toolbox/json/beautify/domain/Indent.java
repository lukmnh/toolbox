package io.github.lukmnh.toolbox.json.beautify.domain;

public enum Indent {
    TWO_SPACES("  "),
    FOUR_SPACES("    "),
    TAB("\t");

    public static final Indent DEFAULT = TWO_SPACES;

    private final String unit;

    Indent(String unit) {
        this.unit = unit;
    }

    /** Teks untuk satu level indentasi. */
    public String unit() {
        return unit;
    }
}
