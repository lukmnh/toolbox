package io.github.lukmnh.toolbox.devutils.regex.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Parser tidak boleh crash atau macet untuk input apa pun (pola sedang diketik user). */
class RegexParserFuzzTest {

    private static final String ALPHABET =
            "()[]{}|*+?^$.\\-:=!<>'&#/,0123456789abdkgpPqQEKRuxXcCnstvwWsSdDhzZAGb" + "{} \n\té😀";
    private static final int ITERATIONS = 10_000;
    private static final int MAX_LENGTH = 24;
    private static final long SEED = 20261028L;

    @Test
    void randomPatternsNeverThrow() {
        Random random = new Random(SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            String pattern = randomPattern(random);
            for (RegexFlavor flavor : RegexFlavor.values()) {
                for (Set<RegexFlag> flags : flagVariants(flavor)) {
                    ParseResult result = RegexParser.parse(pattern, flavor, flags);
                    assertEquals(pattern, result.pattern());
                    result.problems()
                            .forEach(problem -> assertTrue(problem.span().end() <= pattern.length()));
                    translateQuietly(result, new RegexRequest(pattern, flavor, flags));
                }
            }
        }
    }

    /** Terjemahan juga tidak boleh crash: hasilnya pola valid atau UnsupportedRegexException. */
    private static void translateQuietly(ParseResult result, RegexRequest request) {
        try {
            EnginePatternFactory.create(result, request);
        } catch (UnsupportedRegexException expected) {
            assertTrue(!expected.problems().isEmpty());
        }
    }

    private static Set<Set<RegexFlag>> flagVariants(RegexFlavor flavor) {
        return switch (flavor) {
            case JAVA -> Set.of(Set.of(), Set.of(RegexFlag.JAVA_COMMENTS));
            case JAVASCRIPT -> Set.of(Set.of(), Set.of(RegexFlag.JS_UNICODE), Set.of(RegexFlag.JS_UNICODE_SETS));
            case GO -> Set.of(Set.of());
            case PHP -> Set.of(Set.of(), Set.of(RegexFlag.PHP_EXTENDED, RegexFlag.PHP_UNGREEDY));
        };
    }

    private static String randomPattern(Random random) {
        int length = random.nextInt(MAX_LENGTH);
        StringBuilder pattern = new StringBuilder();
        int[] codePoints = ALPHABET.codePoints().toArray();
        for (int index = 0; index < length; index++) {
            pattern.appendCodePoint(codePoints[random.nextInt(codePoints.length)]);
        }
        return pattern.toString();
    }
}
