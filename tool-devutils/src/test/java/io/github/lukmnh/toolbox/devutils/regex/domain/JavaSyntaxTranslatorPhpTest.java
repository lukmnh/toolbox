package io.github.lukmnh.toolbox.devutils.regex.domain;

import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.engine;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.findAll;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.finds;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.matcher;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Matcher;
import org.junit.jupiter.api.Test;

/** Perilaku hasil terjemahan PHP (PCRE2) harus sama dengan preg_match. */
class JavaSyntaxTranslatorPhpTest {

    private static final RegexFlavor PHP = RegexFlavor.PHP;

    @Test
    void pythonStyleNamedGroupAndReference() {
        EnginePattern pattern = engine("(?P<year>\\d{4})-(?P=year)", PHP);
        assertEquals(List.of("", "year"), pattern.groupNames());
        Matcher matcher = matcher(pattern, "x 2026-2026");
        assertTrue(matcher.find());
        assertEquals("2026", matcher.group(1));
        assertFalse(finds("(?P<year>\\d{4})-(?P=year)", PHP, "2026-2025"));
    }

    @Test
    void relativeAndBracedReferences() {
        assertTrue(finds("(a)(b)\\g{-1}\\g{1}\\g2", PHP, "abbab"));
        assertTrue(finds("(?<w>x)\\k{w}\\k'w'\\g{w}", PHP, "xxxx"));
    }

    @Test
    void ungreedyModifierFlipsQuantifiers() {
        assertEquals(List.of("<a>", "<b>"), findAll(engine("<.+>", PHP, RegexFlag.PHP_UNGREEDY), "<a><b>"));
        assertEquals(List.of("<a><b>"), findAll(engine("<.+?>", PHP, RegexFlag.PHP_UNGREEDY), "<a><b>"));
    }

    @Test
    void dollarMatchesBeforeFinalLineFeedOnly() {
        assertTrue(finds("a$", PHP, "a\n"));
        assertFalse(finds("a$", PHP, "a\r\n"));
        assertFalse(finds("a$", PHP, "a\n", RegexFlag.PHP_DOLLAR_END_ONLY));
        assertTrue(finds("a$", PHP, "a\nb", RegexFlag.PHP_MULTILINE, RegexFlag.PHP_DOLLAR_END_ONLY));
    }

    @Test
    void dotExcludesOnlyLineFeed() {
        assertTrue(finds("a.b", PHP, "a\rb"));
        assertTrue(finds("a.b", PHP, "a\u0085b"));
        assertFalse(finds("a.b", PHP, "a\nb"));
        assertTrue(finds("a.b", PHP, "a\nb", RegexFlag.PHP_DOTALL));
    }

    @Test
    void multilineCaretUsesLineFeed() {
        assertEquals(List.of("b"), findAll(engine("^b", PHP, RegexFlag.PHP_MULTILINE), "a\nb"));
        assertEquals(List.of(), findAll(engine("^b", PHP, RegexFlag.PHP_MULTILINE), "a\rb"));
    }

    @Test
    void posixClasses() {
        assertEquals(List.of("ab", "1", "cd"), findAll(engine("[[:alpha:]]+|[[:digit:]]", PHP), "ab1cd"));
        assertEquals(List.of("-", "!"), findAll(engine("[^[:alnum:]\\s]", PHP), "a- b!"));
        assertTrue(finds("^[[:word:]]+$", PHP, "a_1"));
    }

    @Test
    void extendedModifierIgnoresWhitespaceAndComments() {
        assertTrue(finds("a b # komentar\n c", PHP, "abc", RegexFlag.PHP_EXTENDED));
        assertFalse(finds("a b # komentar\n c", PHP, "a b c", RegexFlag.PHP_EXTENDED));
    }

    @Test
    void anchoredModifier() {
        assertEquals(List.of("a", "a"), findAll(engine("a", PHP, RegexFlag.PHP_ANCHORED), "aaba"));
    }

    @Test
    void utf8ModifierMakesClassesUnicode() {
        assertFalse(finds("^\\w$", PHP, "é"));
        assertTrue(finds("^\\w$", PHP, "é", RegexFlag.PHP_UTF8));
        assertTrue(finds("É", PHP, "é", RegexFlag.PHP_UTF8, RegexFlag.PHP_IGNORE_CASE));
    }

    @Test
    void inlineFlagsArePassedThrough() {
        assertTrue(finds("a(?i)b", PHP, "aB"));
        assertFalse(finds("a(?i)b", PHP, "AB"));
        assertTrue(finds("(?s:a.)b", PHP, "a\nb"));
        assertTrue(finds("(?x) a b ", PHP, "ab"));
    }

    @Test
    void escapesAndProperties() {
        assertTrue(finds("\\x41\\x{42}\\o{103}\\103\\e", PHP, "ABCC\u001B"));
        assertTrue(finds("\\N", PHP, "x"));
        assertFalse(finds("^\\N$", PHP, "\n"));
        assertTrue(finds("\\p{Greek}\\p{Lu}\\p{^Ll}", PHP, "λAB"));
        assertTrue(finds("\\h\\R", PHP, "\t\r\n"));
        assertTrue(finds("\\Qa.b\\E", PHP, "a.b"));
        assertFalse(finds("\\Qa.b\\E", PHP, "axb"));
        assertTrue(finds("(?:\\Qab\\E)+", PHP, "abab"));
    }

    @Test
    void featuresWithoutJavaEquivalentAreRejected() {
        UnsupportedRegexException keepOut = assertThrows(UnsupportedRegexException.class, () -> engine("a\\Kb", PHP));
        assertEquals(new Span(1, 3), keepOut.problems().getFirst().span());
        assertEquals(
                Feature.KEEP_OUT.name(), keepOut.problems().getFirst().args().getFirst());
        assertRejected("\\((?:[^()]|(?R))*\\)", Feature.RECURSION);
        assertRejected("(a)(?1)", Feature.SUBROUTINE_CALL);
        assertRejected("(?(1)a|b)", Feature.CONDITIONAL);
        assertRejected("(?|(a)|(b))", Feature.BRANCH_RESET);
        assertRejected("(*SKIP)a", Feature.BACKTRACK_VERB);
        assertRejected("(?U)a+", Feature.INLINE_FLAGS);
        assertRejected("\\p{Xan}", Feature.UNICODE_PROPERTY);
    }

    private static void assertRejected(String pattern, Feature feature) {
        UnsupportedRegexException error = assertThrows(UnsupportedRegexException.class, () -> engine(pattern, PHP));
        assertTrue(
                error.problems().stream().anyMatch(problem -> problem.args().contains(feature.name())),
                () -> pattern + " → " + error.problems());
    }
}
