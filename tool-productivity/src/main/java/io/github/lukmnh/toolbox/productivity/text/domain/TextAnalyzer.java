package io.github.lukmnh.toolbox.productivity.text.domain;

import java.text.BreakIterator;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hitung statistik teks dengan {@link BreakIterator} supaya grapheme (emoji gabungan, huruf + diakritik) dan batas
 * kata/kalimat mengikuti aturan Unicode, bukan sekadar spasi.
 *
 * <p>Teks bisa sampai 10 MB, jadi {@code checkpoint} dipanggil berkala; pemanggil melempar exception di sana untuk
 * membatalkan hitungan yang sudah usang.
 */
public final class TextAnalyzer {

    public static final Locale INDONESIAN = Locale.forLanguageTag("id");
    public static final int READING_WORDS_PER_MINUTE = 200;
    public static final int SPEAKING_WORDS_PER_MINUTE = 130;
    public static final int TOP_WORD_LIMIT = 10;

    /** Jumlah segmen di antara dua pemeriksaan pembatalan; cukup jarang supaya tidak memperlambat loop. */
    static final int CHECKPOINT_INTERVAL = 8_192;

    private static final long SECONDS_PER_MINUTE = 60;
    private static final char HYPHEN = '-';
    private static final char NEW_LINE = '\n';

    private TextAnalyzer() {}

    /** @param ignoreStopWords abaikan kata tugas bahasa Indonesia di daftar kata terbanyak */
    public static TextStatistics analyze(String text, boolean ignoreStopWords, Runnable checkpoint) {
        if (text.isEmpty()) {
            return TextStatistics.EMPTY;
        }
        long[] graphemes = countGraphemes(text, checkpoint);
        WordCollector words = new WordCollector(text, ignoreStopWords);
        words.scan(checkpoint);
        long sentences = countSentences(text, checkpoint);
        long[] lines = countLinesAndParagraphs(text);
        return new TextStatistics(
                graphemes[0],
                graphemes[1],
                words.total(),
                sentences,
                lines[1],
                lines[0],
                duration(words.total(), READING_WORDS_PER_MINUTE),
                duration(words.total(), SPEAKING_WORDS_PER_MINUTE),
                words.top(TOP_WORD_LIMIT));
    }

    /** @return {@code [semua grapheme, grapheme selain spasi]} */
    private static long[] countGraphemes(String text, Runnable checkpoint) {
        BreakIterator iterator = BreakIterator.getCharacterInstance(INDONESIAN);
        iterator.setText(text);
        long total = 0;
        long visible = 0;
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            total++;
            if (!isSpace(text, start, end)) {
                visible++;
            }
            if (total % CHECKPOINT_INTERVAL == 0) {
                checkpoint.run();
            }
        }
        return new long[] {total, visible};
    }

    private static long countSentences(String text, Runnable checkpoint) {
        BreakIterator iterator = BreakIterator.getSentenceInstance(INDONESIAN);
        iterator.setText(text);
        long sentences = 0;
        long segments = 0;
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            if (hasLetterOrDigit(text, start, end)) {
                sentences++;
            }
            if (++segments % CHECKPOINT_INTERVAL == 0) {
                checkpoint.run();
            }
        }
        return sentences;
    }

    /** Paragraf = blok baris tidak kosong yang dipisah minimal satu baris kosong. @return {@code [baris, paragraf]} */
    private static long[] countLinesAndParagraphs(String text) {
        long lines = 0;
        long paragraphs = 0;
        boolean inParagraph = false;
        int start = 0;
        while (start <= text.length()) {
            int end = text.indexOf(NEW_LINE, start);
            int lineEnd = end < 0 ? text.length() : end;
            lines++;
            boolean blank = isSpace(text, start, lineEnd);
            if (!blank && !inParagraph) {
                paragraphs++;
            }
            inParagraph = !blank;
            start = lineEnd + 1;
        }
        return new long[] {lines, paragraphs};
    }

    private static Duration duration(long words, int wordsPerMinute) {
        return Duration.ofSeconds(Math.ceilDiv(words * SECONDS_PER_MINUTE, wordsPerMinute));
    }

    /** Loop index langsung (tanpa {@code substring}) karena dipanggil per segmen untuk teks sampai 10 MB. */
    static boolean hasLetterOrDigit(String text, int start, int end) {
        for (int index = start; index < end; ) {
            int codePoint = text.codePointAt(index);
            if (Character.isLetterOrDigit(codePoint)) {
                return true;
            }
            index += Character.charCount(codePoint);
        }
        return false;
    }

    /** NBSP bukan {@link Character#isWhitespace}, tapi bagi pembaca tetap spasi. */
    private static boolean isSpace(String text, int start, int end) {
        for (int index = start; index < end; ) {
            int codePoint = text.codePointAt(index);
            if (!Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint)) {
                return false;
            }
            index += Character.charCount(codePoint);
        }
        return true;
    }

    /**
     * Kumpulkan kata dari segmen {@link BreakIterator}. Kata ulang ({@code anak-anak}, {@code ke-2}) dipecah
     * BreakIterator di tanda hubung, padahal dalam bahasa Indonesia itu satu kata; segmen yang hanya dipisah satu
     * {@code -} tanpa spasi digabung lagi di sini.
     */
    private static final class WordCollector {

        private final String text;
        private final boolean ignoreStopWords;
        private final Map<String, Long> counts = new HashMap<>();
        private long total;
        private int wordStart = -1;
        private int wordEnd = -1;
        private boolean hyphenPending;

        private WordCollector(String text, boolean ignoreStopWords) {
            this.text = text;
            this.ignoreStopWords = ignoreStopWords;
        }

        void scan(Runnable checkpoint) {
            BreakIterator iterator = BreakIterator.getWordInstance(INDONESIAN);
            iterator.setText(text);
            long segments = 0;
            int start = iterator.first();
            for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                accept(start, end);
                if (++segments % CHECKPOINT_INTERVAL == 0) {
                    checkpoint.run();
                }
            }
            flush();
        }

        private void accept(int start, int end) {
            if (hasLetterOrDigit(text, start, end)) {
                if (wordStart < 0 || !hyphenPending) {
                    flush();
                    wordStart = start;
                }
                wordEnd = end;
                hyphenPending = false;
                return;
            }
            boolean singleHyphen = end - start == 1 && text.charAt(start) == HYPHEN;
            boolean joiningHyphen = singleHyphen && wordStart >= 0 && !hyphenPending && wordEnd == start;
            if (joiningHyphen) {
                hyphenPending = true;
                return;
            }
            flush();
        }

        private void flush() {
            if (wordStart >= 0) {
                record(text.substring(wordStart, wordEnd));
            }
            wordStart = -1;
            hyphenPending = false;
        }

        /** Angka murni ikut dihitung sebagai kata, tapi hanya kata berhuruf yang masuk daftar terbanyak. */
        private void record(String word) {
            total++;
            if (word.codePoints().noneMatch(Character::isLetter)) {
                return;
            }
            String lower = word.toLowerCase(INDONESIAN);
            if (ignoreStopWords && IndonesianStopWords.contains(lower)) {
                return;
            }
            counts.merge(lower, 1L, Long::sum);
        }

        long total() {
            return total;
        }

        List<WordFrequency> top(int limit) {
            return counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(limit)
                    .map(entry -> new WordFrequency(entry.getKey(), entry.getValue()))
                    .toList();
        }
    }
}
