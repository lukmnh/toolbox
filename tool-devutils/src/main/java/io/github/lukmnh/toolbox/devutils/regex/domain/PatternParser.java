package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Inti parser: alternation, urutan, quantifier, dan atom. Grup diserahkan ke {@link GroupParser}. */
final class PatternParser {

    private final ParseCursor in;
    private final ParserDialect dialect;
    private final GroupState groupState;
    private final EscapeParser escapes;
    private final ClassParser classes;
    private final GroupParser groupParser;
    private final QuantifierParser quantifiers;
    private final Map<Integer, GroupInfo> groups = new TreeMap<>();
    private final Map<String, Integer> names = new HashMap<>();
    private final List<RegexNode.Backreference> references = new ArrayList<>();
    private boolean extended;
    private int depth;

    PatternParser(String pattern, ParserDialect dialect) {
        this.in = new ParseCursor(pattern);
        this.dialect = dialect;
        this.groupState = new GroupState(GroupPrescan.scan(pattern, dialect.nestedClasses()));
        this.escapes = new EscapeParser(in, dialect, groupState);
        this.classes = new ClassParser(in, dialect, escapes);
        this.groupParser = new GroupParser(in, dialect, this);
        this.quantifiers = new QuantifierParser(in, dialect);
        this.extended = dialect.extendedInitially();
    }

    ParseResult parse() {
        RegexNode root = parseAlternation();
        new ReferenceValidator(in, dialect, groups, names).validate(references);
        return new ParseResult(in.pattern(), dialect.flavor(), root, List.copyOf(groups.values()), in.problems());
    }

    RegexNode parseAlternation() {
        int start = in.position();
        List<RegexNode> branches = new ArrayList<>();
        branches.add(parseSequence());
        while (in.consume('|')) {
            branches.add(parseSequence());
        }
        if (branches.size() == 1) {
            return branches.getFirst();
        }
        return new RegexNode.Alternation(in.spanFrom(start), branches);
    }

    private RegexNode parseSequence() {
        int start = in.position();
        List<RegexNode> items = new ArrayList<>();
        while (true) {
            skipExtended(items);
            if (in.atEnd() || in.peek() == '|' || (in.peek() == ')' && depth > 0)) {
                break;
            }
            if (!quantifiers.applyIfPresent(items)) {
                parseAtom().ifPresent(items::add);
            }
        }
        return new RegexNode.Sequence(in.spanFrom(start), items);
    }

    /** Mode extended (Java COMMENTS, PCRE x): spasi diabaikan, {@code #} sampai akhir baris jadi komentar. */
    private void skipExtended(List<RegexNode> items) {
        while (extended && !in.atEnd()) {
            if (Character.isWhitespace(in.peek())) {
                in.advance(1);
            } else if (in.peek() == '#') {
                int start = in.position();
                while (!in.atEnd() && in.peek() != '\n') {
                    in.advance(1);
                }
                items.add(new RegexNode.Comment(in.spanFrom(start), in.pattern().substring(start + 1, in.position())));
            } else {
                return;
            }
        }
    }

    private Optional<RegexNode> parseAtom() {
        int start = in.position();
        return switch (in.peek()) {
            case '(' -> Optional.of(groupParser.parse());
            case ')' -> Optional.of(strayClose(start));
            case '[' -> Optional.of(classes.parse(extended));
            case '.' -> simple(start, new RegexNode.AnyChar(new Span(start, start + 1)));
            case '^' -> simple(start, new RegexNode.Anchor(new Span(start, start + 1), AnchorKind.LINE_START));
            case '$' -> simple(start, new RegexNode.Anchor(new Span(start, start + 1), AnchorKind.LINE_END));
            case '\\' -> escapes.parse(false).map(this::track);
            case '{' -> Optional.of(literalBrace(start));
            default -> Optional.of(literal(start));
        };
    }

    private Optional<RegexNode> simple(int start, RegexNode node) {
        in.reset(start + 1);
        return Optional.of(node);
    }

    private RegexNode literal(int start) {
        int codePoint = in.nextCodePoint();
        return new RegexNode.Literal(in.spanFrom(start), codePoint);
    }

    /** {@code )} tanpa pasangan di tingkat atas. */
    private RegexNode strayClose(int start) {
        in.advance(1);
        in.problem(in.spanFrom(start), ProblemCode.UNMATCHED_CLOSE_PAREN);
        return new RegexNode.Unknown(in.spanFrom(start), ")");
    }

    /** {@code {} yang bukan quantifier: literal di Go/PHP/JS Annex B, error di Java dan JS mode Unicode. */
    private RegexNode literalBrace(int start) {
        in.advance(1);
        Span span = in.spanFrom(start);
        if (!dialect.literalBrace()) {
            in.problem(span, ProblemCode.NOTHING_TO_REPEAT);
        }
        return new RegexNode.Literal(span, '{');
    }

    /** Catat backreference bernama/bernomor untuk divalidasi setelah seluruh grup diketahui. */
    RegexNode track(RegexNode node) {
        if (node instanceof RegexNode.Backreference reference) {
            references.add(reference);
        }
        return node;
    }

    int openCapture() {
        return groupState.openNext();
    }

    void registerGroup(int number, String name, Span span) {
        groups.put(number, new GroupInfo(number, name, span));
        if (name.isEmpty()) {
            return;
        }
        if (names.putIfAbsent(name, number) != null) {
            ProblemCode code = dialect.is(RegexFlavor.JAVASCRIPT)
                    ? ProblemCode.UNSUPPORTED_BY_ENGINE
                    : ProblemCode.DUPLICATE_GROUP_NAME;
            String argument = code == ProblemCode.UNSUPPORTED_BY_ENGINE ? Feature.DUPLICATE_GROUP_NAME.name() : name;
            in.problem(span, code, argument);
        }
    }

    boolean extended() {
        return extended;
    }

    void setExtended(boolean value) {
        extended = value;
    }

    void enterGroup() {
        depth++;
    }

    void exitGroup() {
        depth--;
    }
}
