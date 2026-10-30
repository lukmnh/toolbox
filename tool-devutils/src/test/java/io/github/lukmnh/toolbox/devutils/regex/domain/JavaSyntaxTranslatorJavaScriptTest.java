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

/** Perilaku hasil terjemahan JavaScript harus sama dengan engine ECMAScript, bukan default java.util.regex. */
class JavaSyntaxTranslatorJavaScriptTest {

    private static final RegexFlavor JS = RegexFlavor.JAVASCRIPT;

    @Test
    void dollarWithoutMultilineIsEndOfInputOnly() {
        assertFalse(finds("foo$", JS, "foo\n"));
        assertTrue(finds("foo$", JS, "foo"));
    }

    @Test
    void dotFollowsEcmaScriptLineTerminators() {
        assertTrue(finds("a.b", JS, "a\u0085b"));
        assertFalse(finds("a.b", JS, "a b"));
        assertFalse(finds("a.b", JS, "a\rb"));
        assertTrue(finds("a.b", JS, "a\nb", RegexFlag.JS_DOT_ALL));
    }

    @Test
    void whitespaceClassIsUnicodeAware() {
        assertTrue(finds("\\s", JS, " "));
        assertTrue(finds("\\s", JS, "﻿"));
        assertFalse(finds("\\s", JS, "\u0085"));
        assertTrue(finds("[\\s,]", JS, "　"));
        assertFalse(finds("\\S", JS, " "));
        assertTrue(finds("[^\\s]", JS, "x"));
    }

    @Test
    void emptyAndNegatedEmptyClasses() {
        assertTrue(finds("[^]", JS, "\n"));
        assertFalse(finds("a[]", JS, "a"));
    }

    @Test
    void multilineAnchorsUseAllLineTerminators() {
        assertEquals(List.of("b"), findAll(engine("^b", JS, RegexFlag.JS_GLOBAL, RegexFlag.JS_MULTILINE), "a\rb"));
        assertEquals(List.of("a"), findAll(engine("a$", JS, RegexFlag.JS_GLOBAL, RegexFlag.JS_MULTILINE), "a b"));
        assertFalse(finds("^b", JS, "a\rb"));
    }

    @Test
    void stickyMatchesOnlyAtCurrentPosition() {
        EnginePattern sticky = engine("a", JS, RegexFlag.JS_GLOBAL, RegexFlag.JS_STICKY);
        assertEquals(List.of("a", "a"), findAll(sticky, "aab a"));
    }

    @Test
    void globalFlagControlsFirstMatchOnly() {
        assertTrue(engine("a", JS).firstMatchOnly());
        assertFalse(engine("a", JS, RegexFlag.JS_GLOBAL).firstMatchOnly());
    }

    @Test
    void namedGroupsKeepOriginalNames() {
        EnginePattern pattern = engine("(?<$tahun_1>\\d{4})-\\k<$tahun_1>", JS, RegexFlag.JS_GLOBAL);
        assertEquals(List.of("", "$tahun_1"), pattern.groupNames());
        Matcher matcher = matcher(pattern, "2026-2026");
        assertTrue(matcher.find());
        assertEquals("2026", matcher.group(1));
    }

    @Test
    void modifierGroupChangesCaseOnlyInside() {
        assertTrue(finds("(?i:a)b", JS, "Ab"));
        assertFalse(finds("(?i:a)b", JS, "AB"));
        assertTrue(finds("(?-i:a)b", JS, "aB", RegexFlag.JS_IGNORE_CASE));
        assertFalse(finds("(?-i:a)b", JS, "AB", RegexFlag.JS_IGNORE_CASE));
    }

    @Test
    void modifierGroupDotAll() {
        assertTrue(finds("(?s:a.)b", JS, "a\nb"));
        assertFalse(finds("a.(?s:b)", JS, "a\nb"));
    }

    @Test
    void ignoreCaseIsUnicodeAware() {
        assertTrue(finds("é", JS, "É", RegexFlag.JS_IGNORE_CASE));
    }

    @Test
    void unicodeSetsSubtraction() {
        EnginePattern pattern = engine("[\\p{L}--[a-z]]+", JS, RegexFlag.JS_GLOBAL, RegexFlag.JS_UNICODE_SETS);
        assertEquals(List.of("ABC", "É"), findAll(pattern, "ABCdefÉ"));
    }

    @Test
    void unicodePropertiesAreMapped() {
        assertTrue(finds("\\p{Script=Greek}", JS, "λ", RegexFlag.JS_UNICODE));
        assertTrue(finds("\\p{Lowercase_Letter}", JS, "λ", RegexFlag.JS_UNICODE));
        assertTrue(finds("^\\P{Letter}$", JS, "7", RegexFlag.JS_UNICODE));
        assertTrue(finds("\\p{Emoji_Presentation}", JS, "😀", RegexFlag.JS_UNICODE));
        assertTrue(finds("\\u{1F600}", JS, "x😀", RegexFlag.JS_UNICODE));
    }

    @Test
    void literalsAreEscapedForJava() {
        assertTrue(finds("a\\.b\\*[\\]\\-^&]", JS, "a.b*&"));
        assertFalse(finds("a\\.b", JS, "axb"));
        assertTrue(finds("\\0", JS, "\u0000"));
        assertTrue(finds("[\\b]", JS, "\b"));
        assertTrue(finds("{", JS, "{"));
    }

    @Test
    void backreferenceFollowedByDigit() {
        assertTrue(finds("(a)\\1[0]", JS, "aa0"));
        // Di JavaScript \10 tanpa 10 grup adalah octal (backspace), bukan backreference + "0"
        assertTrue(finds("(a)\\10", JS, "a\b"));
    }

    @Test
    void unboundedLookbehindIsUnsupported() {
        UnsupportedRegexException error = assertThrows(UnsupportedRegexException.class, () -> engine("(?<=a+)b", JS));
        assertEquals(
                Feature.UNBOUNDED_LOOKBEHIND.name(),
                error.problems().getFirst().args().getFirst());
        assertTrue(finds("(?<=ab)c", JS, "abc"));
    }

    @Test
    void parserProblemsBlockTranslation() {
        UnsupportedRegexException error = assertThrows(UnsupportedRegexException.class, () -> engine("a++", JS));
        assertEquals(new Span(1, 3), error.problems().getFirst().span());
    }
}
