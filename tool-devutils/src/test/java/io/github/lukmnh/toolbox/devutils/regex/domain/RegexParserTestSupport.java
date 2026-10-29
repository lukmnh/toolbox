package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bantuan test parser/translator: parse, kumpulkan node, dan jalankan hasil terjemahan di java.util.regex. */
final class RegexParserTestSupport {

    private RegexParserTestSupport() {}

    static ParseResult parse(String pattern, RegexFlavor flavor, RegexFlag... flags) {
        return RegexParser.parse(pattern, flavor, flagSet(flags));
    }

    static Set<RegexFlag> flagSet(RegexFlag... flags) {
        return flags.length == 0 ? Set.of() : EnumSet.of(flags[0], flags);
    }

    static EnginePattern engine(String pattern, RegexFlavor flavor, RegexFlag... flags) {
        RegexRequest request = new RegexRequest(pattern, flavor, flagSet(flags));
        return EnginePatternFactory.create(RegexParser.parse(pattern, flavor, request.flags()), request);
    }

    static Matcher matcher(EnginePattern pattern, String text) {
        return Pattern.compile(pattern.source(), pattern.flags()).matcher(text);
    }

    /** Semua match (teks) seperti {@code String.match(/re/g)} / {@code preg_match_all}. */
    static List<String> findAll(EnginePattern pattern, String text) {
        Matcher matcher = matcher(pattern, text);
        List<String> found = new ArrayList<>();
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }

    static boolean finds(String pattern, RegexFlavor flavor, String text, RegexFlag... flags) {
        return matcher(engine(pattern, flavor, flags), text).find();
    }

    /** Semua node di pohon (pre-order), termasuk isi class. */
    static List<RegexNode> nodes(RegexNode root) {
        List<RegexNode> all = new ArrayList<>();
        collect(root, all);
        return all;
    }

    private static void collect(RegexNode node, List<RegexNode> all) {
        all.add(node);
        switch (node) {
            case RegexNode.Alternation alternation -> alternation.branches().forEach(child -> collect(child, all));
            case RegexNode.Sequence sequence -> sequence.items().forEach(child -> collect(child, all));
            case RegexNode.Group group -> collect(group.body(), all);
            case RegexNode.Quantified quantified -> collect(quantified.target(), all);
            case RegexNode.CharClass charClass -> charClass.items().forEach(child -> collect(child, all));
            default -> {
                // Daun
            }
        }
    }

    static <T extends RegexNode> List<T> nodesOf(ParseResult result, Class<T> type) {
        return nodes(result.root()).stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    static <T extends RegexNode> T single(ParseResult result, Class<T> type) {
        List<T> found = nodesOf(result, type);
        if (found.size() != 1) {
            throw new AssertionError("Harus tepat satu " + type.getSimpleName() + ", ada " + found + " di " + result);
        }
        return found.getFirst();
    }

    static boolean hasProblem(ParseResult result, ProblemCode code, String argument) {
        return result.problems().stream()
                .anyMatch(problem -> problem.code() == code
                        && (argument == null || problem.args().contains(argument)));
    }
}
