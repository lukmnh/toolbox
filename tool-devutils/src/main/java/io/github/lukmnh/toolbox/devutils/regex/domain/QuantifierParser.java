package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.List;
import java.util.Optional;

/** Quantifier {@code * + ? {n} {n,} {n,m}} beserta mode malas ({@code ?}) dan posesif ({@code +}). */
final class QuantifierParser {

    /** Batas repeat RE2/Go. */
    private static final int GO_MAX_REPEAT = 1000;

    private static final int MAX_DIGITS = 10;

    private final ParseCursor in;
    private final ParserDialect dialect;

    QuantifierParser(ParseCursor in, ParserDialect dialect) {
        this.in = in;
        this.dialect = dialect;
    }

    /**
     * Bila ada quantifier di posisi sekarang, terapkan ke item terakhir yang bisa diulang.
     *
     * @return {@code true} bila quantifier dikonsumsi
     */
    boolean applyIfPresent(List<RegexNode> items) {
        int start = in.position();
        Optional<Bounds> bounds = readBounds();
        if (bounds.isEmpty()) {
            return false;
        }
        QuantifierMode mode = readMode(start);
        Span quantifierSpan = in.spanFrom(start);
        int targetIndex = lastRepeatable(items);
        if (targetIndex < 0) {
            in.problem(quantifierSpan, ProblemCode.NOTHING_TO_REPEAT);
            items.add(new RegexNode.Unknown(quantifierSpan, in.textFrom(start)));
            return true;
        }
        RegexNode target = items.get(targetIndex);
        int max = validatedMax(bounds.get(), quantifierSpan);
        Span span = new Span(target.span().start(), in.position());
        items.set(
                targetIndex, new RegexNode.Quantified(span, target, bounds.get().min(), max, mode));
        return true;
    }

    private int validatedMax(Bounds bounds, Span span) {
        boolean unbounded = bounds.max() == RegexNode.Quantified.UNBOUNDED;
        if (!unbounded && bounds.max() < bounds.min()) {
            in.problem(span, ProblemCode.INVALID_QUANTIFIER_RANGE, str(bounds.min()), str(bounds.max()));
            return bounds.min();
        }
        boolean tooLarge = bounds.min() > GO_MAX_REPEAT || (!unbounded && bounds.max() > GO_MAX_REPEAT);
        if (dialect.is(RegexFlavor.GO) && tooLarge) {
            in.problem(span, ProblemCode.INVALID_QUANTIFIER_RANGE, str(bounds.min()), str(bounds.max()));
        }
        return bounds.max();
    }

    private static String str(int value) {
        return value == RegexNode.Quantified.UNBOUNDED ? "∞" : Integer.toString(value);
    }

    private QuantifierMode readMode(int start) {
        if (in.consume('?')) {
            return QuantifierMode.LAZY;
        }
        if (in.peek() != '+') {
            return QuantifierMode.GREEDY;
        }
        in.advance(1);
        if (!dialect.possessive()) {
            in.unsupported(in.spanFrom(start), Feature.POSSESSIVE_QUANTIFIER);
        }
        return QuantifierMode.POSSESSIVE;
    }

    /** Item terakhir selain komentar; -1 bila tidak ada yang bisa diulang. */
    private int lastRepeatable(List<RegexNode> items) {
        for (int index = items.size() - 1; index >= 0; index--) {
            RegexNode item = items.get(index);
            if (item instanceof RegexNode.Comment) {
                continue;
            }
            return repeatable(item) ? index : -1;
        }
        return -1;
    }

    private boolean repeatable(RegexNode item) {
        return switch (item) {
            case RegexNode.Quantified ignored -> false;
            case RegexNode.InlineFlags ignored -> false;
            case RegexNode.Anchor ignored -> !dialect.is(RegexFlavor.JAVASCRIPT);
            case RegexNode.Group group -> repeatableGroup(group);
            default -> true;
        };
    }

    /** JavaScript: lookbehind tidak boleh diulang; lookahead hanya di Annex B. */
    private boolean repeatableGroup(RegexNode.Group group) {
        if (!dialect.is(RegexFlavor.JAVASCRIPT)) {
            return true;
        }
        return switch (group.kind()) {
            case LOOKBEHIND, NEGATIVE_LOOKBEHIND -> false;
            case LOOKAHEAD, NEGATIVE_LOOKAHEAD -> dialect.annexB();
            default -> true;
        };
    }

    private Optional<Bounds> readBounds() {
        return switch (in.peek()) {
            case '*' -> single(0, RegexNode.Quantified.UNBOUNDED);
            case '+' -> single(1, RegexNode.Quantified.UNBOUNDED);
            case '?' -> single(0, 1);
            case '{' -> braces();
            default -> Optional.empty();
        };
    }

    private Optional<Bounds> single(int min, int max) {
        in.advance(1);
        return Optional.of(new Bounds(min, max));
    }

    /** {@code {n}}, {@code {n,}}, {@code {n,m}}, dan (PHP) {@code {,m}}; selain itu bukan quantifier. */
    private Optional<Bounds> braces() {
        int resume = in.position();
        in.advance(1);
        int min = in.readNumber(MAX_DIGITS);
        boolean comma = in.consume(',');
        int max = comma ? in.readNumber(MAX_DIGITS) : min;
        boolean noMinimumAllowed = min < 0 && comma && max >= 0 && dialect.braceWithoutMinimum();
        boolean valid = (min >= 0 || noMinimumAllowed) && in.consume('}');
        if (!valid) {
            in.reset(resume);
            return Optional.empty();
        }
        int lower = Math.max(min, 0);
        int upper = comma && max < 0 ? RegexNode.Quantified.UNBOUNDED : max;
        return Optional.of(new Bounds(lower, upper));
    }

    private record Bounds(int min, int max) {}
}
