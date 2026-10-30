package io.github.lukmnh.toolbox.devutils.sql.domain;

public enum IndentStyle {
    TWO_SPACES("  "),
    FOUR_SPACES("    "),
    TAB("\t");

    private final String text;

    IndentStyle(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
