package io.github.lukmnh.toolbox.devutils.regex.domain;

import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.engine;
import static io.github.lukmnh.toolbox.devutils.regex.domain.RegexParserTestSupport.flagSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.Test;

class EnginePatternFactoryTest {

    @Test
    void javaIsPassedThroughWithFlags() {
        EnginePattern pattern = engine(
                "(?<x>a)b",
                RegexFlavor.JAVA,
                RegexFlag.JAVA_CASE_INSENSITIVE,
                RegexFlag.JAVA_MULTILINE,
                RegexFlag.JAVA_UNICODE_CHARACTER_CLASS);
        assertEquals(EngineKind.JDK, pattern.engine());
        assertEquals("(?<x>a)b", pattern.source());
        assertEquals(Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.UNICODE_CHARACTER_CLASS, pattern.flags());
        assertEquals(List.of("", "x"), pattern.groupNames());
        assertEquals(5, pattern.offsets().toOriginal(5));
        assertFalse(pattern.firstMatchOnly());
    }

    @Test
    void javaNeverThrowsEvenWithProblems() {
        EnginePattern pattern = engine("a(", RegexFlavor.JAVA);
        assertEquals("a(", pattern.source());
    }

    @Test
    void goGetsFlagPrefixAndShiftedOffsets() {
        EnginePattern pattern = engine(
                "a(?P<n>b)", RegexFlavor.GO, RegexFlag.GO_UNGREEDY, RegexFlag.GO_IGNORE_CASE, RegexFlag.GO_DOT_NEWLINE);
        assertEquals(EngineKind.RE2, pattern.engine());
        assertEquals("(?isU)a(?P<n>b)", pattern.source());
        assertEquals(0, pattern.flags());
        assertEquals(0, pattern.offsets().toOriginal(3));
        assertEquals(1, pattern.offsets().toOriginal(7));
        assertEquals(List.of("", "n"), pattern.groupNames());
        assertEquals("a", engine("a", RegexFlavor.GO).source());
    }

    @Test
    void translatedOffsetsPointBackToOriginalPattern() {
        EnginePattern php = engine("a\\p{Nope}", RegexFlavor.PHP);
        PatternSyntaxException error =
                assertThrows(PatternSyntaxException.class, () -> Pattern.compile(php.source(), php.flags()));
        int original = php.offsets().toOriginal(error.getIndex());
        assertTrue(original >= 1 && original <= "a\\p{Nope}".length(), () -> "posisi " + original);
        assertEquals(0, php.offsets().toOriginal(0));
    }

    @Test
    void mismatchedRequestIsRejected() {
        RegexRequest request = new RegexRequest("a", RegexFlavor.JAVA, flagSet());
        ParseResult other = RegexParser.parse("b", RegexFlavor.JAVA, flagSet());
        assertThrows(IllegalArgumentException.class, () -> EnginePatternFactory.create(other, request));
    }
}
