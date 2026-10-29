package io.github.lukmnh.toolbox.devutils.regex.domain;

import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.hasProblem;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.nodesOf;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.parse;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.single;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RegexParserGoTest {

    private static ParseResult go(String pattern) {
        return parse(pattern, RegexFlavor.GO);
    }

    @Test
    void bothNamedGroupSyntaxes() {
        ParseResult result = go("(?P<tahun>\\d{4})-(?<bulan>\\d{2})");
        assertEquals(List.of("", "tahun", "bulan"), result.groupNames());
        assertFalse(result.hasProblems());
    }

    @Test
    void posixPropertiesAndQuote() {
        ParseResult result = go("[[:alpha:][:^digit:]]\\pN\\p{Greek}\\Qa.b\\E\\x{10FFFF}\\x7F\\123");
        List<RegexNode.PosixClass> posix = nodesOf(result, RegexNode.PosixClass.class);
        assertEquals(2, posix.size());
        assertTrue(posix.get(1).negated());
        assertEquals(2, nodesOf(result, RegexNode.UnicodeProperty.class).size());
        assertEquals(
                List.of(0x10FFFF, 0x7F, 0123),
                nodesOf(result, RegexNode.Literal.class).stream()
                        .map(RegexNode.Literal::codePoint)
                        .toList());
        assertFalse(result.hasProblems());
    }

    @Test
    void featuresMissingInRe2() {
        assertTrue(hasProblem(go("a(?=b)"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.LOOKAHEAD.name()));
        assertTrue(hasProblem(go("(?<!a)b"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.LOOKBEHIND.name()));
        assertTrue(hasProblem(go("(a)\\1"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.BACKREFERENCE.name()));
        assertTrue(hasProblem(go("(?>a)"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.ATOMIC_GROUP.name()));
        assertTrue(hasProblem(go("a*+"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.POSSESSIVE_QUANTIFIER.name()));
        assertTrue(hasProblem(go("\\Z"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.END_BEFORE_NEWLINE_ANCHOR.name()));
        assertTrue(hasProblem(go("a{1001}"), ProblemCode.INVALID_QUANTIFIER_RANGE, null));
    }

    @Test
    void flagsAndLiteralBraces() {
        ParseResult result = go("(?i)a(?s-m:.)x{,2}");
        assertEquals("i", single(result, RegexNode.InlineFlags.class).flagsOn());
        assertTrue(nodesOf(result, RegexNode.Literal.class).stream().anyMatch(l -> l.codePoint() == '{'));
        assertFalse(result.hasProblems());
        assertTrue(hasProblem(go("(?x)"), ProblemCode.UNKNOWN_FLAG, "x"));
    }

    @Test
    void verticalTabEscapeIsLiteral() {
        assertEquals(0x0B, single(go("\\v"), RegexNode.Literal.class).codePoint());
    }
}
