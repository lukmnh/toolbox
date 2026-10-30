package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.Set;

/**
 * Parser pola regex per flavor (recursive descent, tanpa memakai regex untuk mem-parse regex). Toleran: selalu
 * mengembalikan pohon, bagian rusak menjadi {@link RegexNode.Unknown} dan dicatat sebagai {@link RegexProblem}.
 *
 * <p>Untuk Java dan Go hasil parse hanya untuk penjelasan; validitas tetap ditentukan engine aslinya. Untuk JavaScript
 * dan PHP, masalah yang ditemukan di sini menentukan apakah pola bisa diterjemahkan.
 */
public final class RegexParser {

    private RegexParser() {}

    public static ParseResult parse(String pattern, RegexFlavor flavor, Set<RegexFlag> flags) {
        return new PatternParser(pattern, ParserDialect.of(flavor, flags)).parse();
    }
}
