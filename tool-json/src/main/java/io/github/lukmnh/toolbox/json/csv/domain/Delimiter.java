package io.github.lukmnh.toolbox.json.csv.domain;

/** Pemisah kolom yang didukung. Urutan enum = prioritas saat skor deteksi seri. */
public enum Delimiter {
    COMMA(','),
    SEMICOLON(';'),
    TAB('\t'),
    PIPE('|');

    private final char symbol;

    Delimiter(char symbol) {
        this.symbol = symbol;
    }

    public char symbol() {
        return symbol;
    }
}
