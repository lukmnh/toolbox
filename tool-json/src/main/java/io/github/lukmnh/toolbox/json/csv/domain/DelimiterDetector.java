package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Memilih pemisah yang jumlah kolom per record-nya paling konsisten di sampel. Jumlah kolom dihitung oleh parser CSV
 * (quote dihormati), sehingga {@code "Jakarta, Indonesia"} tidak dihitung sebagai dua kolom.
 */
public final class DelimiterDetector {

    /** Bila tidak ada kandidat yang memecah kolom, koma (standar RFC 4180) dipakai. */
    public static final Delimiter FALLBACK = Delimiter.COMMA;

    private DelimiterDetector() {}

    /** @param fieldCounts jumlah kolom per record sampel untuk setiap kandidat pemisah */
    public static Delimiter choose(Map<Delimiter, List<Integer>> fieldCounts) {
        Delimiter best = FALLBACK;
        Score bestScore = Score.NONE;
        for (Delimiter candidate : Delimiter.values()) {
            Score score = score(fieldCounts.getOrDefault(candidate, List.of()));
            // Hanya skor yang lebih baik yang menggantikan, jadi seri dimenangkan urutan enum
            if (score.isBetterThan(bestScore)) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static Score score(List<Integer> counts) {
        Map<Integer, Integer> frequency = new HashMap<>();
        counts.forEach(count -> frequency.merge(count, 1, Integer::sum));
        int modeCount = 0;
        int modeFrequency = 0;
        for (Map.Entry<Integer, Integer> entry : frequency.entrySet()) {
            boolean moreFrequent = entry.getValue() > modeFrequency;
            boolean tieWithMoreColumns = entry.getValue() == modeFrequency && entry.getKey() > modeCount;
            if (moreFrequent || tieWithMoreColumns) {
                modeCount = entry.getKey();
                modeFrequency = entry.getValue();
            }
        }
        // Satu kolom berarti pemisah ini tidak muncul sama sekali
        if (modeCount <= 1) {
            return Score.NONE;
        }
        return new Score((double) modeFrequency / counts.size(), modeCount);
    }

    /** Konsistensi (porsi record dengan jumlah kolom modus) lalu jumlah kolom modus. */
    private record Score(double consistency, int columns) {

        static final Score NONE = new Score(0, 0);

        boolean isBetterThan(Score other) {
            if (consistency != other.consistency) {
                return consistency > other.consistency;
            }
            return columns > other.columns;
        }
    }
}
