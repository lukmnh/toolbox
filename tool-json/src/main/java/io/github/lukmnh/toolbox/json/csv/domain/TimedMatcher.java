package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.List;
import java.util.regex.Pattern;

/** Port: eksekusi regex dengan batas waktu, supaya pola user tidak bisa membekukan app (Security Requirements #3). */
@FunctionalInterface
public interface TimedMatcher {

    /**
     * Capture group 1..n dari match pertama {@code pattern} di {@code value}. Group yang tidak ikut match bernilai
     * string kosong.
     *
     * @return list kosong bila tidak ada match
     * @throws RegexSplitException bila melewati batas waktu atau terlalu kompleks
     */
    List<String> captureGroups(Pattern pattern, String value);
}
