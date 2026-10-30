package io.github.lukmnh.toolbox.productivity.shared.domain;

/** Gaya huruf yang didukung {@link CaseConverter}. Urutan enum = urutan tampil. */
public enum CaseStyle {
    UPPER,
    LOWER,
    SENTENCE,
    TITLE,
    CAMEL,
    PASCAL,
    SNAKE,
    KEBAB,
    CONSTANT,
    DOT;

    /** Gaya identifier: teks dipecah jadi kata lalu digabung ulang, spasi & tanda baca hilang. */
    public boolean joinsWords() {
        return switch (this) {
            case CAMEL, PASCAL, SNAKE, KEBAB, CONSTANT, DOT -> true;
            case UPPER, LOWER, SENTENCE, TITLE -> false;
        };
    }
}
