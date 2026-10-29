package io.github.lukmnh.toolbox.devutils.regex.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReplacementTemplateTest {

    /** Input "x 2024-05 y": match "2024-05" di posisi 2..9, grup 1 = "2024", grup 2 = "05". */
    private static final String INPUT = "x 2024-05 y";

    private static RegexMatch dateMatch(List<String> names) {
        List<GroupMatch> groups = new ArrayList<>();
        groups.add(new GroupMatch(1, names.get(1), 2, 6, "2024"));
        groups.add(new GroupMatch(2, names.get(2), 7, 9, "05"));
        return new RegexMatch(2, 9, "2024-05", groups);
    }

    private static String replace(String template, RegexFlavor flavor, List<String> names) {
        return ReplacementTemplate.parse(template, flavor, names).replaceAll(INPUT, List.of(dateMatch(names)));
    }

    @Nested
    class Java {

        private final List<String> names = List.of("", "year", "");

        private String apply(String template) {
            return replace(template, RegexFlavor.JAVA, names);
        }

        @Test
        void numberedNamedAndEscapedReferences() {
            assertEquals("x 05/2024 y", apply("$2/$1"));
            assertEquals("x 2024! y", apply("${year}!"));
            assertEquals("x $1 y", apply("\\$1"));
            assertEquals("x 2024-05 y", apply("$0"));
        }

        @Test
        void digitsAreTakenWhileTheGroupExists() {
            assertEquals("x 20242 y", apply("$12"));
        }

        @ParameterizedTest
        @CsvSource(
                delimiterString = "=>",
                value = {
                    "$3 => 0 => $3",
                    "a${nope} => 1 => ${nope}",
                    "a$ => 1 => $",
                    "$x => 0 => $x",
                    "${1} => 0 => ${1}"
                })
        void invalidReferencesAreErrors(String template, int position, String reference) {
            InvalidReplacementException error = assertThrows(
                    InvalidReplacementException.class,
                    () -> ReplacementTemplate.parse(template, RegexFlavor.JAVA, names));
            assertEquals(position, error.position());
            assertEquals(reference, error.reference());
        }

        @Test
        void trailingBackslashIsError() {
            assertThrows(
                    InvalidReplacementException.class, () -> ReplacementTemplate.parse("a\\", RegexFlavor.JAVA, names));
        }
    }

    @Nested
    class JavaScript {

        private final List<String> names = List.of("", "y", "");

        private String apply(String template) {
            return replace(template, RegexFlavor.JAVASCRIPT, names);
        }

        @Test
        void specialPatterns() {
            assertEquals("x 05-2024 y", apply("$2-$1"));
            assertEquals("x $ y", apply("$$"));
            assertEquals("x [2024-05] y", apply("[$&]"));
            assertEquals("x <x > y", apply("<$`>"));
            assertEquals("x < y> y", apply("<$'>"));
        }

        @Test
        void numberedReferenceRules() {
            assertEquals("x $0 y", apply("$0"));
            assertEquals("x 2024 y", apply("$01"));
            assertEquals("x 20240 y", apply("$10"));
            assertEquals("x $3 y", apply("$3"));
            assertEquals("x $00 y", apply("$00"));
            assertEquals("x a$ y", apply("a$"));
        }

        @Test
        void namedReferences() {
            assertEquals("x 2024 y", apply("$<y>"));
            assertEquals("x  y", apply("$<zz>"));
            assertEquals("x $<y y", apply("$<y"));
            assertEquals("x $<x> y", replace("$<x>", RegexFlavor.JAVASCRIPT, List.of("", "", "")));
        }

        @Test
        void unmatchedGroupIsEmpty() {
            RegexMatch match = new RegexMatch(
                    2, 9, "2024-05", List.of(new GroupMatch(1, "y", -1, -1, ""), new GroupMatch(2, "", 7, 9, "05")));
            assertEquals(
                    "x [] y",
                    ReplacementTemplate.parse("[$1]", RegexFlavor.JAVASCRIPT, names)
                            .replaceAll(INPUT, List.of(match)));
        }
    }

    @Nested
    class Go {

        private final List<String> names = List.of("", "first", "");

        private String apply(String template) {
            return replace(template, RegexFlavor.GO, names);
        }

        @Test
        void namesAreTheLongestRun() {
            assertEquals("x  y", apply("$1x"));
            assertEquals("x 2024x y", apply("${1}x"));
            assertEquals("x 2024 y", apply("$first"));
            assertEquals("x 2024- y", apply("${first}-"));
            assertEquals("x 05 y", apply("$2"));
            assertEquals("x 2024-05 y", apply("$0"));
        }

        @Test
        void missingAndMalformedReferences() {
            assertEquals("x  y", apply("$9"));
            assertEquals("x  y", apply("$01"));
            assertEquals("x $ y", apply("$$"));
            assertEquals("x $! y", apply("$!"));
            assertEquals("x ${1 y", apply("${1"));
            assertEquals("x a$ y", apply("a$"));
        }
    }

    @Nested
    class Php {

        private final List<String> names = List.of("", "", "");

        private String apply(String template) {
            return replace(template, RegexFlavor.PHP, names);
        }

        @Test
        void backslashAndDollarReferences() {
            assertEquals("x 2024-05 y", apply("\\1-$2"));
            assertEquals("x 20240 y", apply("${1}0"));
            assertEquals("x 2024-05|2024-05 y", apply("$0|\\0"));
            assertEquals("x  y", apply("$9"));
            assertEquals("x  y", apply("\\11"));
        }

        @Test
        void escapesAndLiterals() {
            assertEquals("x \\1 y", apply("\\\\1"));
            assertEquals("x $1 y", apply("\\$1"));
            assertEquals("x $x y", apply("$x"));
            assertEquals("x a\\ y", apply("a\\"));
            assertEquals("x ${1 y", apply("${1"));
        }
    }

    @Test
    void replacesEveryMatchInOrder() {
        List<RegexMatch> matches = List.of(
                new RegexMatch(0, 1, "a", List.of(new GroupMatch(1, "", 0, 1, "a"))),
                new RegexMatch(2, 3, "b", List.of(new GroupMatch(1, "", 2, 3, "b"))));
        ReplacementTemplate template = ReplacementTemplate.parse("<$1>", RegexFlavor.JAVA, List.of("", ""));
        assertEquals("<a>-<b>-", template.replaceAll("a-b-", matches));
        assertEquals("tanpa", template.replaceAll("tanpa", List.of()));
    }
}
