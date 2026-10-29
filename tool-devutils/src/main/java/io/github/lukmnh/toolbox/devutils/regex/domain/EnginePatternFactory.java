package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Menyiapkan pola untuk engine. Java dan Go dikirim apa adanya (engine aslinya yang menentukan validitas); JavaScript
 * dan PHP diterjemahkan ke sintaks {@code java.util.regex}.
 */
public final class EnginePatternFactory {

    private static final String GO_FLAG_ORDER = "imsU";

    private EnginePatternFactory() {}

    /**
     * @throws UnsupportedRegexException JavaScript/PHP dengan error sintaks atau fitur tanpa padanan di engine Java
     */
    public static EnginePattern create(ParseResult parsed, RegexRequest request) {
        if (parsed.flavor() != request.flavor() || !parsed.pattern().equals(request.pattern())) {
            throw new IllegalArgumentException("Hasil parse tidak sesuai dengan request");
        }
        return switch (request.flavor()) {
            case JAVA -> java(parsed, request);
            case GO -> go(parsed, request);
            case JAVASCRIPT, PHP -> translated(parsed, request);
        };
    }

    private static EnginePattern java(ParseResult parsed, RegexRequest request) {
        int flags = 0;
        for (RegexFlag flag : request.flags()) {
            flags |= javaConstant(flag);
        }
        String pattern = request.pattern();
        return new EnginePattern(
                EngineKind.JDK, pattern, flags, parsed.groupNames(), OffsetMap.identity(pattern.length()), false);
    }

    private static int javaConstant(RegexFlag flag) {
        return switch (flag) {
            case JAVA_CASE_INSENSITIVE -> Pattern.CASE_INSENSITIVE;
            case JAVA_MULTILINE -> Pattern.MULTILINE;
            case JAVA_DOTALL -> Pattern.DOTALL;
            case JAVA_COMMENTS -> Pattern.COMMENTS;
            case JAVA_UNICODE_CASE -> Pattern.UNICODE_CASE;
            case JAVA_UNICODE_CHARACTER_CLASS -> Pattern.UNICODE_CHARACTER_CLASS;
            default -> 0;
        };
    }

    /** Paket regexp Go tidak punya parameter flag; flag ditulis sebagai prefix {@code (?ims)}. */
    private static EnginePattern go(ParseResult parsed, RegexRequest request) {
        StringBuilder letters = new StringBuilder();
        for (char letter : GO_FLAG_ORDER.toCharArray()) {
            boolean active =
                    request.flags().stream().anyMatch(flag -> flag.token().equals(String.valueOf(letter)));
            if (active) {
                letters.append(letter);
            }
        }
        String prefix = letters.isEmpty() ? "" : "(?" + letters + ")";
        String pattern = request.pattern();
        return new EnginePattern(
                EngineKind.RE2,
                prefix + pattern,
                0,
                parsed.groupNames(),
                OffsetMap.withPrefix(prefix.length(), pattern.length()),
                false);
    }

    private static EnginePattern translated(ParseResult parsed, RegexRequest request) {
        JavaSyntaxTranslator.Translation translation = JavaSyntaxTranslator.translate(parsed, request);
        List<RegexProblem> problems = new ArrayList<>(parsed.problems());
        problems.addAll(translation.problems());
        if (!problems.isEmpty()) {
            throw new UnsupportedRegexException(problems);
        }
        boolean firstOnly = request.flavor() == RegexFlavor.JAVASCRIPT && !request.has(RegexFlag.JS_GLOBAL);
        return new EnginePattern(
                EngineKind.JDK,
                translation.source(),
                translation.flags(),
                parsed.groupNames(),
                translation.offsets(),
                firstOnly);
    }
}
