package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Character class {@code [...]}: range, escape, POSIX, class bersarang, dan operator {@code &&}/{@code --}. */
final class ClassParser {

    private final ParseCursor in;
    private final ParserDialect dialect;
    private final EscapeParser escapes;
    private boolean extended;

    ClassParser(ParseCursor in, ParserDialect dialect, EscapeParser escapes) {
        this.in = in;
        this.dialect = dialect;
        this.escapes = escapes;
    }

    /** @param extendedMode Java COMMENTS juga mengabaikan spasi di dalam class */
    RegexNode parse(boolean extendedMode) {
        this.extended = extendedMode && dialect.is(RegexFlavor.JAVA);
        int start = in.position();
        in.advance(1);
        boolean negated = in.consume('^');
        List<RegexNode> items = new ArrayList<>();
        boolean first = true;
        while (true) {
            skipJavaWhitespace();
            if (in.atEnd()) {
                in.problem(new Span(start, start + 1), ProblemCode.UNTERMINATED_CLASS);
                break;
            }
            if (in.peek() == ']' && !(first && dialect.closingBracketFirstIsLiteral())) {
                in.advance(1);
                break;
            }
            first = false;
            parseItem(items);
        }
        return new RegexNode.CharClass(in.spanFrom(start), negated, items);
    }

    private void skipJavaWhitespace() {
        while (extended && !in.atEnd() && Character.isWhitespace(in.peek())) {
            in.advance(1);
        }
    }

    private void parseItem(List<RegexNode> items) {
        int start = in.position();
        if (in.peek() == '[') {
            items.add(bracket(start));
            return;
        }
        Optional<SetOperator> operator = operator();
        if (operator.isPresent()) {
            items.add(new RegexNode.ClassOperator(in.spanFrom(start), operator.get()));
            return;
        }
        Optional<RegexNode> atom = atom(items);
        atom.ifPresent(node -> items.add(rangeOrSelf(node)));
    }

    private Optional<SetOperator> operator() {
        if (dialect.classIntersection() && in.consume("&&")) {
            return Optional.of(SetOperator.INTERSECTION);
        }
        if (dialect.unicodeSets() && in.consume("--")) {
            return Optional.of(SetOperator.SUBTRACTION);
        }
        return Optional.empty();
    }

    /** {@code [} di dalam class: POSIX ({@code [:alpha:]}), class bersarang (Java, JS v), atau literal. */
    private RegexNode bracket(int start) {
        Optional<RegexNode> posix = posix(start);
        if (posix.isPresent()) {
            return posix.get();
        }
        if (dialect.nestedClasses()) {
            return parse(extended);
        }
        in.advance(1);
        return new RegexNode.Literal(in.spanFrom(start), '[');
    }

    private Optional<RegexNode> posix(int start) {
        if (!in.lookingAt("[:")) {
            return Optional.empty();
        }
        int nameStart = start + 2;
        int close = in.pattern().indexOf(":]", nameStart);
        if (close < 0 || !isPosixName(in.pattern().substring(nameStart, close))) {
            return Optional.empty();
        }
        String raw = in.pattern().substring(nameStart, close);
        boolean negated = raw.startsWith("^");
        Span span = new Span(start, close + 2);
        if (!dialect.posixClasses()) {
            // Java dan JS membacanya sebagai karakter biasa; beri tahu user karena hampir pasti salah kira
            in.unsupported(span, Feature.POSIX_CLASS);
            return Optional.empty();
        }
        in.reset(close + 2);
        String name = negated ? raw.substring(1) : raw;
        if (!PosixClasses.isKnown(name)) {
            in.problem(span, ProblemCode.INVALID_ESCAPE, in.textFrom(start));
        }
        return Optional.of(new RegexNode.PosixClass(span, name, negated));
    }

    private static boolean isPosixName(String raw) {
        String name = raw.startsWith("^") ? raw.substring(1) : raw;
        return !name.isEmpty() && name.chars().allMatch(ParseCursor::isAsciiLetter);
    }

    /** Satu karakter/escape. {@code \\Q..\\E} di dalam class dipecah menjadi literal per karakter. */
    private Optional<RegexNode> atom(List<RegexNode> items) {
        int start = in.position();
        if (in.peek() != '\\') {
            int codePoint = in.nextCodePoint();
            return Optional.of(new RegexNode.Literal(in.spanFrom(start), codePoint));
        }
        if (dialect.unicodeSets() && in.lookingAt("\\q{")) {
            return Optional.of(classString(start));
        }
        Optional<RegexNode> escaped = escapes.parse(true);
        if (escaped.isPresent() && escaped.get() instanceof RegexNode.QuotedLiteral quoted) {
            expandQuote(quoted, items);
            return Optional.empty();
        }
        return escaped;
    }

    /** JS flag v {@code \\q{abc|def}}: alternatif string di dalam class, tidak ada padanannya di Java. */
    private RegexNode classString(int start) {
        int close = in.pattern().indexOf('}', start);
        in.reset(close < 0 ? in.pattern().length() : close + 1);
        Span span = in.spanFrom(start);
        in.problem(span, ProblemCode.UNSUPPORTED_BY_ENGINE, Feature.NESTED_CLASS.name());
        return new RegexNode.Unknown(span, in.textFrom(start));
    }

    private static void expandQuote(RegexNode.QuotedLiteral quoted, List<RegexNode> items) {
        int offset = quoted.span().start() + 2;
        int index = 0;
        while (index < quoted.text().length()) {
            int codePoint = quoted.text().codePointAt(index);
            int width = Character.charCount(codePoint);
            items.add(new RegexNode.Literal(new Span(offset + index, offset + index + width), codePoint));
            index += width;
        }
    }

    /** {@code a-z}: hanya bila kedua sisi karakter tunggal; selain itu {@code -} literal. */
    private RegexNode rangeOrSelf(RegexNode from) {
        boolean dashStartsRange = in.peek() == '-' && in.peek(1) != ']' && in.peek(1) != ParseCursor.END;
        boolean jsSubtraction = dialect.unicodeSets() && in.peek(1) == '-';
        if (!(from instanceof RegexNode.Literal low) || !dashStartsRange || jsSubtraction) {
            return from;
        }
        int resume = in.position();
        int problemCount = in.problemCount();
        in.advance(1);
        if (in.peek() == '[') {
            in.reset(resume);
            return from;
        }
        List<RegexNode> scratch = new ArrayList<>();
        Optional<RegexNode> to = atom(scratch);
        if (to.isEmpty() || !(to.get() instanceof RegexNode.Literal high)) {
            in.reset(resume);
            in.truncateProblems(problemCount);
            return from;
        }
        Span span = new Span(low.span().start(), high.span().end());
        if (low.codePoint() > high.codePoint()) {
            in.problem(span, ProblemCode.INVALID_CLASS_RANGE, describe(low), describe(high));
        }
        return new RegexNode.ClassRange(span, low.codePoint(), high.codePoint());
    }

    private static String describe(RegexNode.Literal literal) {
        return new String(Character.toChars(literal.codePoint()));
    }
}
