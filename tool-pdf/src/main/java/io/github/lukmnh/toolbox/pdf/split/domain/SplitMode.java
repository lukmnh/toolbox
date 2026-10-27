package io.github.lukmnh.toolbox.pdf.split.domain;

import java.util.Objects;

/** Cara memisah PDF. */
public sealed interface SplitMode permits SplitMode.EachPage, SplitMode.Ranges {

    /** Satu file per halaman. */
    record EachPage() implements SplitMode {}

    /** Satu file per range, misalnya {@code "1-3, 5, 8-10"}. Divalidasi setelah jumlah halaman diketahui. */
    record Ranges(String specification) implements SplitMode {
        public Ranges {
            Objects.requireNonNull(specification, "specification");
        }
    }
}
