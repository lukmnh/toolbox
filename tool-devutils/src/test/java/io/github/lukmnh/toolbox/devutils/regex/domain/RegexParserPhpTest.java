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

class RegexParserPhpTest {

    private static ParseResult php(String pattern, RegexFlag... flags) {
        return parse(pattern, RegexFlavor.PHP, flags);
    }

    @Test
    void namedGroupSyntaxes() {
        ParseResult result = php("(?P<a>x)(?<b>y)(?'c'z)");
        assertEquals(List.of("", "a", "b", "c"), result.groupNames());
        assertFalse(result.hasProblems());
    }

    @Test
    void allBackreferenceForms() {
        ParseResult result = php("(?<y>a)(b)(?P=y)\\k<y>\\k'y'\\k{y}\\g1\\g{2}\\g{-1}\\g{y}\\2");
        List<RegexNode.Backreference> references = nodesOf(result, RegexNode.Backreference.class);
        assertEquals(9, references.size());
        assertEquals(
                List.of("y", "y", "y", "y"),
                references.subList(0, 4).stream()
                        .map(RegexNode.Backreference::name)
                        .toList());
        assertEquals(
                List.of(1, 2, 2),
                references.subList(4, 7).stream()
                        .map(RegexNode.Backreference::number)
                        .toList());
        assertFalse(result.hasProblems());
    }

    @Test
    void largeNumberWithoutGroupsIsOctal() {
        assertEquals(011, single(php("\\11"), RegexNode.Literal.class).codePoint());
        assertEquals(
                11,
                single(php("((((((((((((a))))))))))))\\11"), RegexNode.Backreference.class)
                        .number());
    }

    @Test
    void specialConstructs() {
        ParseResult result = php("a\\Kb(?R)(?1)(?-1)(?&n)(?P>n)\\g<n>(?(1)x|y)(?|a|b)(*SKIP)\\C(?<n>c)");
        List<SpecialKind> kinds = nodesOf(result, RegexNode.SpecialConstruct.class).stream()
                .map(RegexNode.SpecialConstruct::kind)
                .toList();
        assertEquals(
                List.of(
                        SpecialKind.KEEP_OUT,
                        SpecialKind.RECURSION,
                        SpecialKind.SUBROUTINE_CALL,
                        SpecialKind.SUBROUTINE_CALL,
                        SpecialKind.SUBROUTINE_CALL,
                        SpecialKind.SUBROUTINE_CALL,
                        SpecialKind.SUBROUTINE_CALL,
                        SpecialKind.CONDITIONAL,
                        SpecialKind.BRANCH_RESET,
                        SpecialKind.BACKTRACK_VERB,
                        SpecialKind.SINGLE_BYTE),
                kinds);
        assertFalse(result.hasProblems());
    }

    @Test
    void posixClassesAndLiteralClosingBracket() {
        RegexNode.CharClass charClass = single(php("[]a[:alpha:][:^space:]]"), RegexNode.CharClass.class);
        assertEquals(4, charClass.items().size());
        assertEquals(']', ((RegexNode.Literal) charClass.items().getFirst()).codePoint());
        assertTrue(hasProblem(php("[[:nope:]]"), ProblemCode.INVALID_ESCAPE, null));
    }

    @Test
    void extendedModeSkipsWhitespaceAndComments() {
        ParseResult result = php("a b # komentar\n c [ ]", RegexFlag.PHP_EXTENDED);
        assertEquals(
                List.of((int) 'a', (int) 'b', (int) 'c', (int) ' '),
                nodesOf(result, RegexNode.Literal.class).stream()
                        .map(RegexNode.Literal::codePoint)
                        .toList());
        assertEquals(1, nodesOf(result, RegexNode.Comment.class).size());
    }

    @Test
    void escapesAndQuantifiers() {
        ParseResult result = php("\\o{101}\\x\\x4\\e\\N+\\h*+a{,3}\\p{^Lu}\\E");
        List<Integer> literals = nodesOf(result, RegexNode.Literal.class).stream()
                .map(RegexNode.Literal::codePoint)
                .toList();
        assertEquals(List.of(0101, 0, 4, 27, (int) 'a'), literals);
        RegexNode.UnicodeProperty property = single(result, RegexNode.UnicodeProperty.class);
        assertEquals("Lu", property.name());
        assertTrue(property.negated());
        List<RegexNode.Quantified> quantified = nodesOf(result, RegexNode.Quantified.class);
        assertEquals(QuantifierMode.POSSESSIVE, quantified.get(1).mode());
        assertEquals(0, quantified.get(2).min());
        assertEquals(3, quantified.get(2).max());
        assertFalse(result.hasProblems());
    }

    @Test
    void unknownNamedReferenceIsReported() {
        assertTrue(hasProblem(php("(?P=nope)"), ProblemCode.UNKNOWN_GROUP_REFERENCE, "nope"));
        assertTrue(hasProblem(php("\\g{-2}(a)"), ProblemCode.UNKNOWN_GROUP_REFERENCE, null));
        assertTrue(hasProblem(php("(?<a>x)(?<a>y)"), ProblemCode.DUPLICATE_GROUP_NAME, "a"));
    }
}
