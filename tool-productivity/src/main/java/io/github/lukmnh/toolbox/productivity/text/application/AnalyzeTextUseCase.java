package io.github.lukmnh.toolbox.productivity.text.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.productivity.text.domain.TextAnalyzer;
import io.github.lukmnh.toolbox.productivity.text.domain.TextStatistics;

/** Hitung statistik teks (karakter, kata, kalimat, waktu baca, kata terbanyak). */
public final class AnalyzeTextUseCase {

    /**
     * Ketikan baru membatalkan hitungan lama lewat {@code cancellation}.
     *
     * @param ignoreStopWords abaikan kata tugas bahasa Indonesia di daftar kata terbanyak
     */
    public TextStatistics execute(String text, boolean ignoreStopWords, CancellationToken cancellation) {
        return TextAnalyzer.analyze(text, ignoreStopWords, cancellation::throwIfCancelled);
    }
}
