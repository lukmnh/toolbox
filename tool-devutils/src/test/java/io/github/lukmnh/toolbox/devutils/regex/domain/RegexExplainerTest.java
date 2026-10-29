package io.github.lukmnh.toolbox.devutils.regex.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegexExplainerTest {

    private static Span span(int start, int end) {
        return new Span(start, end);
    }

    private static RegexNode.Literal literal(int at, char ch) {
        return new RegexNode.Literal(span(at, at + 1), ch);
    }

    private static List<Explanation> explain(String pattern, RegexNode root, RegexFlavor flavor, Set<RegexFlag> flags) {
        return RegexExplainer.explain(new ParseResult(pattern, flavor, root, List.of(), List.of()), flags);
    }

    private static List<Explanation> explain(String pattern, RegexNode root) {
        return explain(pattern, root, RegexFlavor.JAVA, Set.of());
    }

    @Test
    void quantifiedShorthandIsOneLineWithQuantity() {
        // \d{3,}
        RegexNode root = new RegexNode.Sequence(
                span(0, 6),
                List.of(new RegexNode.Quantified(
                        span(0, 6),
                        new RegexNode.Shorthand(span(0, 2), ShorthandKind.DIGIT),
                        3,
                        RegexNode.Quantified.UNBOUNDED,
                        QuantifierMode.GREEDY)));
        assertEquals(
                List.of(new Explanation(
                        0,
                        ExplanationKind.SHORTHAND,
                        List.of("DIGIT"),
                        new Quantity(3, RegexNode.Quantified.UNBOUNDED, QuantifierMode.GREEDY),
                        span(0, 6),
                        0)),
                explain("\\d{3,}", root));
    }

    @Test
    void emptyPatternHasOneEmptyLine() {
        List<Explanation> lines = explain("", new RegexNode.Sequence(span(0, 0), List.of()));
        assertEquals(1, lines.size());
        assertEquals(ExplanationKind.EMPTY, lines.getFirst().kind());
    }

    @Test
    void consecutiveLiteralsAreMergedButInvisibleOnesStandAlone() {
        // "ab c" lalu "d+"
        RegexNode root = new RegexNode.Sequence(
                span(0, 6),
                List.of(
                        literal(0, 'a'),
                        literal(1, 'b'),
                        literal(2, ' '),
                        literal(3, 'c'),
                        new RegexNode.Quantified(span(4, 6), literal(4, 'd'), 1, -1, QuantifierMode.LAZY)));
        List<Explanation> lines = explain("ab cd+", root);
        assertEquals(4, lines.size());
        assertEquals(List.of("ab"), lines.get(0).args());
        assertEquals(span(0, 2), lines.get(0).span());
        assertEquals(ExplanationKind.LITERAL_SPECIAL, lines.get(1).kind());
        assertEquals(List.of("0020"), lines.get(1).args());
        assertEquals(List.of("c"), lines.get(2).args());
        assertEquals(ExplanationKind.LITERAL_TEXT, lines.get(3).kind());
        assertEquals(new Quantity(1, -1, QuantifierMode.LAZY), lines.get(3).quantity());
        assertEquals(span(4, 6), lines.get(3).span());
    }

    @Test
    void nestedGroupsCarryDepthAndGroupNumber() {
        // (a(?<n>b))+
        RegexNode inner = new RegexNode.Group(
                span(2, 9),
                GroupKind.NAMED,
                2,
                "n",
                "",
                "",
                new RegexNode.Sequence(span(7, 8), List.of(literal(7, 'b'))));
        RegexNode outer = new RegexNode.Group(
                span(0, 10),
                GroupKind.CAPTURING,
                1,
                "",
                "",
                "",
                new RegexNode.Sequence(span(1, 9), List.of(literal(1, 'a'), inner)));
        RegexNode root = new RegexNode.Sequence(
                span(0, 11), List.of(new RegexNode.Quantified(span(0, 11), outer, 1, -1, QuantifierMode.GREEDY)));
        List<Explanation> lines = explain("(a(?<n>b))+", root);

        assertEquals(4, lines.size());
        Explanation group = lines.get(0);
        assertEquals(ExplanationKind.GROUP_CAPTURING, group.kind());
        assertEquals(List.of("1"), group.args());
        assertEquals(1, group.groupNumber());
        assertEquals(span(0, 11), group.span());
        assertEquals(new Quantity(1, -1, QuantifierMode.GREEDY), group.quantity());
        assertEquals(1, lines.get(1).depth());
        assertEquals(ExplanationKind.GROUP_NAMED, lines.get(2).kind());
        assertEquals(List.of("2", "n"), lines.get(2).args());
        assertEquals(2, lines.get(2).groupNumber());
        assertEquals(2, lines.get(3).depth());
    }

    @Test
    void alternationListsEachBranchIncludingEmptyOnes() {
        // a|
        RegexNode root = new RegexNode.Alternation(
                span(0, 2),
                List.of(
                        new RegexNode.Sequence(span(0, 1), List.of(literal(0, 'a'))),
                        new RegexNode.Sequence(span(2, 2), List.of())));
        List<Explanation> lines = explain("a|", root);
        assertEquals(
                List.of(
                        ExplanationKind.ALTERNATION,
                        ExplanationKind.ALTERNATIVE,
                        ExplanationKind.LITERAL_TEXT,
                        ExplanationKind.ALTERNATIVE,
                        ExplanationKind.EMPTY),
                lines.stream().map(Explanation::kind).toList());
        assertEquals(
                List.of(0, 1, 2, 1, 2), lines.stream().map(Explanation::depth).toList());
        assertEquals(List.of("2"), lines.get(3).args());
    }

    @Test
    void dotAllAndMultilineFollowFlagsInlineFlagsAndFlagGroups() {
        // (?s:.)^.(?s).$
        RegexNode flagGroup = new RegexNode.Group(
                span(0, 6), GroupKind.FLAG_GROUP, 0, "", "s", "", new RegexNode.AnyChar(span(4, 5)));
        RegexNode root = new RegexNode.Sequence(
                span(0, 14),
                List.of(
                        flagGroup,
                        new RegexNode.Anchor(span(6, 7), AnchorKind.LINE_START),
                        new RegexNode.AnyChar(span(7, 8)),
                        new RegexNode.InlineFlags(span(8, 12), "s", ""),
                        new RegexNode.AnyChar(span(12, 13)),
                        new RegexNode.Anchor(span(13, 14), AnchorKind.LINE_END)));
        List<Explanation> lines =
                explain("(?s:.)^.(?s).$", root, RegexFlavor.JAVASCRIPT, Set.of(RegexFlag.JS_MULTILINE));

        assertEquals(List.of("s", ""), lines.get(0).args());
        assertEquals(List.of("true"), lines.get(1).args());
        assertEquals(List.of("LINE_START", "true"), lines.get(2).args());
        assertEquals(List.of("false"), lines.get(3).args());
        assertEquals(ExplanationKind.INLINE_FLAGS, lines.get(4).kind());
        assertEquals(List.of("true"), lines.get(5).args());
    }

    @Test
    void flavorFlagSetsInitialDotAll() {
        RegexNode root = new RegexNode.Sequence(span(0, 1), List.of(new RegexNode.AnyChar(span(0, 1))));
        assertEquals(
                List.of("true"),
                explain(".", root, RegexFlavor.PHP, Set.of(RegexFlag.PHP_DOTALL))
                        .getFirst()
                        .args());
        assertEquals(List.of("false"), explain(".", root).getFirst().args());
    }

    @Test
    void characterClassItems() {
        // [^a-z\d\t&&x]
        RegexNode root = new RegexNode.CharClass(
                span(0, 13),
                true,
                List.of(
                        new RegexNode.ClassRange(span(2, 5), 'a', 'z'),
                        new RegexNode.Shorthand(span(5, 7), ShorthandKind.DIGIT),
                        new RegexNode.Literal(span(7, 9), '\t'),
                        new RegexNode.ClassOperator(span(9, 11), SetOperator.INTERSECTION),
                        literal(11, 'x')));
        List<Explanation> lines = explain("[^a-z\\d\\t&&x]", root);
        assertEquals(
                List.of(
                        ExplanationKind.NEGATED_CHAR_CLASS,
                        ExplanationKind.CLASS_RANGE,
                        ExplanationKind.SHORTHAND,
                        ExplanationKind.LITERAL_SPECIAL,
                        ExplanationKind.CLASS_OPERATOR,
                        ExplanationKind.CLASS_LITERAL),
                lines.stream().map(Explanation::kind).toList());
        assertEquals(List.of("a", "z"), lines.get(1).args());
        assertEquals(List.of("0009"), lines.get(3).args());
        assertEquals(List.of("INTERSECTION"), lines.get(4).args());
        assertEquals(1, lines.get(5).depth());
    }

    @Test
    void leafKinds() {
        RegexNode root = new RegexNode.Sequence(
                span(0, 20),
                List.of(
                        new RegexNode.Backreference(span(0, 2), 1, ""),
                        new RegexNode.Backreference(span(2, 7), 0, "nama"),
                        new RegexNode.SpecialConstruct(span(7, 9), SpecialKind.KEEP_OUT, "\\K"),
                        new RegexNode.Unknown(span(9, 12), "(*X"),
                        new RegexNode.QuotedLiteral(span(12, 17), "a.b"),
                        new RegexNode.UnicodeProperty(span(17, 20), "L", true)));
        List<Explanation> lines = explain("pola", root);
        assertEquals(List.of("1"), lines.get(0).args());
        assertEquals(ExplanationKind.BACKREFERENCE_NAME, lines.get(1).kind());
        assertEquals(List.of("KEEP_OUT", "\\K"), lines.get(2).args());
        assertEquals(List.of("(*X"), lines.get(3).args());
        assertEquals(List.of("a.b"), lines.get(4).args());
        assertEquals(List.of("L", "true"), lines.get(5).args());
    }
}
