package io.github.lukmnh.toolbox.productivity.text.domain;

import java.util.Objects;

/** Satu kata (huruf kecil) beserta jumlah kemunculannya. */
public record WordFrequency(String word, long count) {

    public WordFrequency {
        Objects.requireNonNull(word, "word");
        if (word.isEmpty() || count < 1) {
            throw new IllegalArgumentException("Empty word or count < 1");
        }
    }
}
