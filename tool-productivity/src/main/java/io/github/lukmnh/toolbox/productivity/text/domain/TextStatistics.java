package io.github.lukmnh.toolbox.productivity.text.domain;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Statistik satu teks.
 *
 * @param characters jumlah grapheme (emoji gabungan dihitung 1)
 * @param charactersWithoutSpaces grapheme yang bukan spasi/tab/baris baru
 * @param topWords maksimal {@link TextAnalyzer#TOP_WORD_LIMIT} kata, terbanyak dulu
 */
public record TextStatistics(
        long characters,
        long charactersWithoutSpaces,
        long words,
        long sentences,
        long paragraphs,
        long lines,
        Duration readingTime,
        Duration speakingTime,
        List<WordFrequency> topWords) {

    public static final TextStatistics EMPTY =
            new TextStatistics(0, 0, 0, 0, 0, 0, Duration.ZERO, Duration.ZERO, List.of());

    public TextStatistics {
        boolean negative = characters < 0
                || charactersWithoutSpaces < 0
                || words < 0
                || sentences < 0
                || paragraphs < 0
                || lines < 0;
        if (negative) {
            throw new IllegalArgumentException("Statistik tidak boleh negatif");
        }
        Objects.requireNonNull(readingTime, "readingTime");
        Objects.requireNonNull(speakingTime, "speakingTime");
        topWords = List.copyOf(topWords);
    }
}
