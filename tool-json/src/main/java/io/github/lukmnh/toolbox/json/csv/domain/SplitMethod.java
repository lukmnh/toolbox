package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.Objects;

/** Cara memecah satu nilai. Teks aturan disimpan mentah dan divalidasi saat pipeline di-compile. */
public sealed interface SplitMethod {

    /** Pecah di setiap kemunculan teks literal (bukan regex). */
    record BySeparator(String separator) implements SplitMethod {

        public BySeparator {
            Objects.requireNonNull(separator, "separator");
        }
    }

    /** Pecah di posisi karakter tetap, misalnya {@code "3, 8"} → [0,3) [3,8) [8,akhir). */
    record ByPositions(String positions) implements SplitMethod {

        public ByPositions {
            Objects.requireNonNull(positions, "positions");
        }
    }

    /** Ambil capture group dari match pertama; tiap group jadi satu kolom. */
    record ByRegex(String pattern) implements SplitMethod {

        public ByRegex {
            Objects.requireNonNull(pattern, "pattern");
        }
    }
}
