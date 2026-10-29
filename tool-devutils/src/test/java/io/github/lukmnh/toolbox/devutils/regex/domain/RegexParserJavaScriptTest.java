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

class RegexParserJavaScriptTest {

    private static ParseResult js(String pattern, RegexFlag... flags) {
        return parse(pattern, RegexFlavor.JAVASCRIPT, flags);
    }

    private static ParseResult jsUnicode(String pattern) {
        return parse(pattern, RegexFlavor.JAVASCRIPT, RegexFlag.JS_UNICODE);
    }

    @Test
    void possessiveQuantifierIsReportedAtQuantifier() {
        ParseResult result = js("a++");
        RegexProblem problem = result.problems().getFirst();
        assertEquals(ProblemCode.UNSUPPORTED_BY_FLAVOR, problem.code());
        assertEquals(List.of(Feature.POSSESSIVE_QUANTIFIER.name()), problem.args());
        assertEquals(1, problem.span().start());
    }

    @Test
    void constructsMissingInJavaScript() {
        assertTrue(hasProblem(js("(?>a)"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.ATOMIC_GROUP.name()));
        assertTrue(hasProblem(js("(?i)a"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.INLINE_FLAGS.name()));
        assertTrue(hasProblem(js("(?#c)"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.COMMENT_GROUP.name()));
        assertTrue(hasProblem(js("\\Aa"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.INPUT_ANCHOR.name()));
        assertTrue(hasProblem(jsUnicode("\\Qa\\E"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.QUOTE.name()));
        assertTrue(hasProblem(js("\\h"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.HORIZONTAL_VERTICAL_SPACE.name()));
        assertTrue(hasProblem(js("\\R"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.LINEBREAK.name()));
        assertTrue(hasProblem(js("[[:alpha:]]"), ProblemCode.UNSUPPORTED_BY_FLAVOR, Feature.POSIX_CLASS.name()));
    }

    @Test
    void modifierGroupIsAllowed() {
        ParseResult result = js("(?i-s:a)b");
        RegexNode.Group group = single(result, RegexNode.Group.class);
        assertEquals(GroupKind.FLAG_GROUP, group.kind());
        assertEquals("i", group.flagsOn());
        assertEquals("s", group.flagsOff());
        assertFalse(result.hasProblems());
        assertTrue(hasProblem(js("(?x:a)"), ProblemCode.UNKNOWN_FLAG, "x"));
    }

    @Test
    void namedGroupsAllowDollarAndUnderscore() {
        ParseResult result = js("(?<$tahun_1>\\d{4})\\k<$tahun_1>");
        assertEquals(List.of("", "$tahun_1"), result.groupNames());
        assertEquals("$tahun_1", single(result, RegexNode.Backreference.class).name());
        assertFalse(result.hasProblems());
    }

    @Test
    void jsSpecificCharacters() {
        assertEquals(0x0B, single(js("\\v"), RegexNode.Literal.class).codePoint());
        assertEquals(0x08, single(js("[\\b]"), RegexNode.Literal.class).codePoint());
        assertEquals(0, single(js("\\0"), RegexNode.Literal.class).codePoint());
        assertEquals(
                0x1F600,
                single(jsUnicode("\\u{1F600}"), RegexNode.Literal.class).codePoint());
        assertEquals(
                0x1F600,
                single(jsUnicode("\\uD83D\\uDE00"), RegexNode.Literal.class).codePoint());
        assertTrue(single(js("[^]"), RegexNode.CharClass.class).items().isEmpty());
        assertTrue(single(js("[]"), RegexNode.CharClass.class).items().isEmpty());
    }

    @Test
    void annexBIsLenientWithoutUnicodeFlag() {
        assertEquals('{', single(js("{"), RegexNode.Literal.class).codePoint());
        assertEquals(List.of((int) 'a', (int) '{', (int) '1'), codePoints(js("a{1")));
        assertEquals('p', single(js("\\p"), RegexNode.Literal.class).codePoint());
        assertEquals('k', single(js("\\k"), RegexNode.Literal.class).codePoint());
        assertEquals('8', single(js("\\8"), RegexNode.Literal.class).codePoint());
        assertEquals(012, single(js("\\12"), RegexNode.Literal.class).codePoint());
        assertFalse(js("\\@\\y").hasProblems());
    }

    @Test
    void unicodeModeIsStrict() {
        assertTrue(hasProblem(jsUnicode("{"), ProblemCode.NOTHING_TO_REPEAT, null));
        assertTrue(hasProblem(jsUnicode("\\y"), ProblemCode.INVALID_ESCAPE, "\\y"));
        assertTrue(hasProblem(jsUnicode("\\2"), ProblemCode.INVALID_ESCAPE, null));
        assertEquals(
                "Script=Greek",
                single(jsUnicode("\\p{Script=Greek}"), RegexNode.UnicodeProperty.class)
                        .name());
    }

    @Test
    void backreferenceCanPointForward() {
        ParseResult result = js("\\1(a)");
        assertEquals(1, single(result, RegexNode.Backreference.class).number());
        assertFalse(result.hasProblems());
    }

    @Test
    void unicodeSetsSyntax() {
        ParseResult result = js("[\\p{L}--[a-z]&&[\\w]]", RegexFlag.JS_UNICODE_SETS);
        List<SetOperator> operators = nodesOf(result, RegexNode.ClassOperator.class).stream()
                .map(RegexNode.ClassOperator::operator)
                .toList();
        assertEquals(List.of(SetOperator.SUBTRACTION, SetOperator.INTERSECTION), operators);
        assertEquals(3, nodesOf(result, RegexNode.CharClass.class).size());
        assertFalse(result.hasProblems());
    }

    @Test
    void lookbehindCannotBeQuantified() {
        assertTrue(hasProblem(js("(?<=a)*"), ProblemCode.NOTHING_TO_REPEAT, null));
        assertTrue(hasProblem(js("^*"), ProblemCode.NOTHING_TO_REPEAT, null));
    }

    private static List<Integer> codePoints(ParseResult result) {
        return nodesOf(result, RegexNode.Literal.class).stream()
                .map(RegexNode.Literal::codePoint)
                .toList();
    }
}
