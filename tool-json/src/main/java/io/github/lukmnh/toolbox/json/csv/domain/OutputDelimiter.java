package io.github.lukmnh.toolbox.json.csv.domain;

/** Pilihan pemisah hasil: sama dengan input atau pemisah tertentu. */
public enum OutputDelimiter {
    SAME_AS_INPUT,
    COMMA,
    SEMICOLON,
    TAB,
    PIPE;

    public Delimiter resolve(Delimiter input) {
        return switch (this) {
            case SAME_AS_INPUT -> input;
            case COMMA -> Delimiter.COMMA;
            case SEMICOLON -> Delimiter.SEMICOLON;
            case TAB -> Delimiter.TAB;
            case PIPE -> Delimiter.PIPE;
        };
    }
}
