package io.github.lukmnh.toolbox.devutils.regex.domain;

import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.hasProblem;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.nodesOf;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.parse;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.single;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RegexParserJavaTest {

    private static ParseResult java(String pattern, RegexFlag... flags) {
        return parse(pattern, RegexFlavor.JAVA, flags);
    }

    @Test
    void emptyPatternIsEmptySequence() {
        ParseResult result = java("");
        RegexNode.Sequence root = assertInstanceOf(RegexNode.Sequence.class, result.root());
        assertTrue(root.items().isEmpty());
        assertFalse(result.hasProblems());
    }

    @Test
    void alternationAndSequenceSpans() {
        RegexNode.Alternation root =
                assertInstanceOf(RegexNode.Alternation.class, java("ab|c|").root());
        assertEquals(3, root.branches().size());
        assertEquals(new Span(0, 5), root.span());
        assertEquals(new Span(0, 2), root.branches().get(0).span());
        assertTrue(((RegexNode.Sequence) root.branches().get(2)).items().isEmpty());
    }

    @Test
    void groupsAreNumberedByOpeningParenthesis() {
        ParseResult result = java("((a)(?<name>b))(?:c)(?>d)(?=e)(?!f)(?<=g)(?<!h)");
        assertEquals(List.of("", "", "", "name"), result.groupNames());
        List<RegexNode.Group> groups = nodesOf(result, RegexNode.Group.class);
        assertEquals(
                List.of(
                        GroupKind.CAPTURING,
                        GroupKind.CAPTURING,
                        GroupKind.NAMED,
                        GroupKind.NON_CAPTURING,
                        GroupKind.ATOMIC,
                        GroupKind.LOOKAHEAD,
                        GroupKind.NEGATIVE_LOOKAHEAD,
                        GroupKind.LOOKBEHIND,
                        GroupKind.NEGATIVE_LOOKBEHIND),
                groups.stream().map(RegexNode.Group::kind).toList());
        assertEquals(3, groups.get(2).number());
        assertFalse(result.hasProblems());
    }

    @Test
    void quantifiersWithModes() {
        ParseResult result = java("a*b+?c?+d{2}e{2,}f{2,5}?");
        List<RegexNode.Quantified> quantified = nodesOf(result, RegexNode.Quantified.class);
        assertEquals(6, quantified.size());
        assertEquals(QuantifierMode.GREEDY, quantified.get(0).mode());
        assertEquals(QuantifierMode.LAZY, quantified.get(1).mode());
        assertEquals(QuantifierMode.POSSESSIVE, quantified.get(2).mode());
        assertEquals(2, quantified.get(3).max());
        assertEquals(RegexNode.Quantified.UNBOUNDED, quantified.get(4).max());
        assertEquals(5, quantified.get(5).max());
        assertEquals(QuantifierMode.LAZY, quantified.get(5).mode());
        assertFalse(result.hasProblems());
    }

    @Test
    void classWithIntersectionAndNestedClass() {
        RegexNode.CharClass outer = (RegexNode.CharClass)
                ((RegexNode.Sequence) java("[a-z&&[^aeiou]]").root()).items().getFirst();
        assertInstanceOf(RegexNode.ClassRange.class, outer.items().get(0));
        assertInstanceOf(RegexNode.ClassOperator.class, outer.items().get(1));
        RegexNode.CharClass inner =
                assertInstanceOf(RegexNode.CharClass.class, outer.items().get(2));
        assertTrue(inner.negated());
        assertEquals(5, inner.items().size());
    }

    @Test
    void escapesAndAnchors() {
        ParseResult result = java("\\t\\x41\\x{1F600}\\u0041\\0101\\cJ\\e\\a\\N{LATIN SMALL LETTER A}");
        List<Integer> codePoints = nodesOf(result, RegexNode.Literal.class).stream()
                .map(RegexNode.Literal::codePoint)
                .toList();
        assertEquals(List.of(9, 0x41, 0x1F600, 0x41, 0101, 10, 27, 7, (int) 'a'), codePoints);
        assertFalse(result.hasProblems());

        List<AnchorKind> anchors = nodesOf(java("^\\A\\z\\Z\\G\\b\\B$"), RegexNode.Anchor.class).stream()
                .map(RegexNode.Anchor::kind)
                .toList();
        assertEquals(8, anchors.size());
    }

    @Test
    void shorthandsPropertiesAndQuote() {
        ParseResult result = java("\\d\\h\\v\\R\\X\\p{javaLowerCase}\\pL\\P{IsLatin}\\Qa.b\\E");
        assertEquals(5, nodesOf(result, RegexNode.Shorthand.class).size());
        assertEquals(3, nodesOf(result, RegexNode.UnicodeProperty.class).size());
        assertEquals("a.b", single(result, RegexNode.QuotedLiteral.class).text());
        assertFalse(result.hasProblems());
    }

    @Test
    void backreferenceDigitsDependOnGroupsSoFar() {
        RegexNode.Backreference reference = single(java("(a)\\11"), RegexNode.Backreference.class);
        assertEquals(1, reference.number());
        assertEquals(
                1,
                nodesOf(java("(a)\\11"), RegexNode.Literal.class).stream()
                        .filter(literal -> literal.codePoint() == '1')
                        .count());
        assertEquals(
                "x",
                single(java("(?<x>a)\\k<x>"), RegexNode.Backreference.class).name());
    }

    @Test
    void inlineFlagsAndCommentsMode() {
        ParseResult result = java("(?i)a(?-i:b)(?x) c # komentar\n d");
        assertEquals(2, nodesOf(result, RegexNode.InlineFlags.class).size());
        assertEquals(1, nodesOf(result, RegexNode.Comment.class).size());
        assertFalse(nodesOf(result, RegexNode.Literal.class).stream().anyMatch(literal -> literal.codePoint() == ' '));
        assertFalse(result.hasProblems());
    }

    @Test
    void syntaxErrorsHaveExactSpans() {
        ParseResult open = java("a(b");
        assertTrue(hasProblem(open, ProblemCode.UNMATCHED_OPEN_PAREN, null));
        assertEquals(new Span(1, 2), open.problems().getFirst().span());
        assertEquals(new Span(1, 2), java("a)").problems().getFirst().span());
        assertTrue(hasProblem(java("*a"), ProblemCode.NOTHING_TO_REPEAT, null));
        assertTrue(hasProblem(java("[z-a]"), ProblemCode.INVALID_CLASS_RANGE, "z"));
        assertTrue(hasProblem(java("[abc"), ProblemCode.UNTERMINATED_CLASS, null));
        assertTrue(hasProblem(java("a\\"), ProblemCode.TRAILING_BACKSLASH, null));
        assertTrue(hasProblem(java("\\y"), ProblemCode.INVALID_ESCAPE, "\\y"));
        assertTrue(hasProblem(java("(?<1a>x)"), ProblemCode.INVALID_GROUP_NAME, "1a"));
        assertTrue(hasProblem(java("(?<a>x)(?<a>y)"), ProblemCode.DUPLICATE_GROUP_NAME, "a"));
        assertTrue(hasProblem(java("\\k<nope>"), ProblemCode.UNKNOWN_GROUP_REFERENCE, "nope"));
        assertTrue(hasProblem(java("(?q)"), ProblemCode.UNKNOWN_FLAG, "q"));
    }

    @Test
    void phpOnlyConstructsAreReportedForJava() {
        assertTrue(hasProblem(java("a\\Kb"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.KEEP_OUT.name()));
        assertTrue(hasProblem(java("(?R)"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.RECURSION.name()));
        assertTrue(hasProblem(java("(?#x)"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.COMMENT_GROUP.name()));
        assertTrue(hasProblem(java("[[:alpha:]]"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.POSIX_CLASS.name()));
    }
}
