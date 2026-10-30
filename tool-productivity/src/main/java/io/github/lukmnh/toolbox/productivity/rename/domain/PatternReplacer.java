package io.github.lukmnh.toolbox.productivity.rename.domain;

import java.util.regex.Pattern;

/** Port eksekusi regex dengan batas waktu, supaya pola dengan catastrophic backtracking tidak membekukan app. */
public interface PatternReplacer {

    /**
     * Setara {@code pattern.matcher(text).replaceAll(replacement)}.
     *
     * @throws InvalidRuleException bila waktu habis atau teks pengganti merujuk grup yang tidak ada
     */
    String replaceAll(Pattern pattern, String text, String replacement);
}
