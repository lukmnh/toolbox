package io.github.lukmnh.toolbox.devutils.regex.domain;

/**
 * Semua bentuk {@code (...)}: capture, bernama, non-capture, atomic, lookaround, flag, komentar, dan konstruk PCRE
 * (rekursi, subroutine, conditional, branch reset, verb).
 */
final class GroupParser {

    private final ParseCursor in;
    private final ParserDialect dialect;
    private final PatternParser parser;
    private final InlineFlagParser flags;

    GroupParser(ParseCursor in, ParserDialect dialect, PatternParser parser) {
        this.in = in;
        this.dialect = dialect;
        this.parser = parser;
        this.flags = new InlineFlagParser(in, dialect, parser, this);
    }

    /** Posisi di {@code (}. */
    RegexNode parse() {
        int start = in.position();
        in.advance(1);
        if (in.peek() == '*' && dialect.is(RegexFlavor.PHP)) {
            return balancedSpecial(start, SpecialKind.BACKTRACK_VERB, Feature.BACKTRACK_VERB);
        }
        if (!in.consume('?')) {
            int number = parser.openCapture();
            return body(start, GroupKind.CAPTURING, number, "", "", "");
        }
        return extension(start);
    }

    private RegexNode extension(int start) {
        int marker = in.peek();
        return switch (marker) {
            case ':' -> simpleGroup(start, GroupKind.NON_CAPTURING);
            case '=' -> lookaround(start, GroupKind.LOOKAHEAD, Feature.LOOKAHEAD);
            case '!' -> lookaround(start, GroupKind.NEGATIVE_LOOKAHEAD, Feature.LOOKAHEAD);
            case '>' -> atomic(start);
            case '<' -> angle(start);
            case 'P' -> pythonStyle(start);
            case '\'' -> quotedName(start);
            case '#' -> comment(start);
            case '|' -> balancedSpecial(start, SpecialKind.BRANCH_RESET, Feature.BRANCH_RESET);
            case '(' -> balancedSpecial(start, SpecialKind.CONDITIONAL, Feature.CONDITIONAL);
            case '&' -> callUntilClose(start, SpecialKind.SUBROUTINE_CALL, Feature.SUBROUTINE_CALL);
            default -> numericOrFlags(start, marker);
        };
    }

    private RegexNode simpleGroup(int start, GroupKind kind) {
        in.advance(1);
        return body(start, kind, 0, "", "", "");
    }

    private RegexNode lookaround(int start, GroupKind kind, Feature feature) {
        if (dialect.is(RegexFlavor.GO)) {
            in.unsupported(new Span(start, in.position() + 1), feature);
        }
        return simpleGroup(start, kind);
    }

    private RegexNode atomic(int start) {
        boolean supported = dialect.is(RegexFlavor.JAVA) || dialect.is(RegexFlavor.PHP);
        if (!supported) {
            in.unsupported(new Span(start, in.position() + 1), Feature.ATOMIC_GROUP);
        }
        return simpleGroup(start, GroupKind.ATOMIC);
    }

    private RegexNode angle(int start) {
        int next = in.peek(1);
        if (next == '=') {
            in.advance(1);
            return lookaround(start, GroupKind.LOOKBEHIND, Feature.LOOKBEHIND);
        }
        if (next == '!') {
            in.advance(1);
            return lookaround(start, GroupKind.NEGATIVE_LOOKBEHIND, Feature.LOOKBEHIND);
        }
        in.advance(1);
        return named(start, '>');
    }

    /** {@code (?P<n>...)}, {@code (?P=n)}, {@code (?P>n)}: gaya Python, didukung Go (P&lt;) dan PHP. */
    private RegexNode pythonStyle(int start) {
        int next = in.peek(1);
        boolean php = dialect.is(RegexFlavor.PHP);
        if (next == '<' && (php || dialect.is(RegexFlavor.GO))) {
            in.advance(2);
            return named(start, '>');
        }
        if (next == '=' && php) {
            in.advance(2);
            return parser.track(nameReference(start));
        }
        if (next == '>' && php) {
            return callUntilClose(start, SpecialKind.SUBROUTINE_CALL, Feature.SUBROUTINE_CALL);
        }
        return invalidSyntax(start);
    }

    private RegexNode nameReference(int start) {
        int nameStart = in.position();
        while (!in.atEnd() && in.peek() != ')') {
            in.advance(1);
        }
        String name = in.pattern().substring(nameStart, in.position());
        if (!in.consume(')') || name.isEmpty()) {
            in.problem(in.spanFrom(start), ProblemCode.INVALID_GROUP_NAME, name);
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        return new RegexNode.Backreference(in.spanFrom(start), 0, name);
    }

    private RegexNode quotedName(int start) {
        if (!dialect.is(RegexFlavor.PHP)) {
            return invalidSyntax(start);
        }
        in.advance(1);
        return named(start, '\'');
    }

    /** Nama sampai {@code close}, lalu isi grup. Nama tidak valid tetap membuat grup supaya parse berlanjut. */
    private RegexNode named(int start, char close) {
        int nameStart = in.position();
        while (!in.atEnd() && in.peek() != close && in.peek() != ')') {
            in.advance(1);
        }
        String name = in.pattern().substring(nameStart, in.position());
        boolean closed = in.consume(close);
        if (!closed || !GroupNames.isValid(dialect.flavor(), name)) {
            in.problem(in.spanFrom(start), ProblemCode.INVALID_GROUP_NAME, name);
        }
        int number = parser.openCapture();
        return body(start, GroupKind.NAMED, number, name, "", "");
    }

    private RegexNode comment(int start) {
        int textStart = in.position() + 1;
        while (!in.atEnd() && in.peek() != ')') {
            in.advance(1);
        }
        String text = in.pattern().substring(Math.min(textStart, in.position()), in.position());
        if (!in.consume(')')) {
            in.problem(new Span(start, start + 1), ProblemCode.UNMATCHED_OPEN_PAREN);
        }
        Span span = in.spanFrom(start);
        if (!dialect.is(RegexFlavor.PHP)) {
            in.unsupported(span, Feature.COMMENT_GROUP);
        }
        return new RegexNode.Comment(span, text);
    }

    /** {@code (?R)}, {@code (?1)}, {@code (?-1)}, {@code (?+1)}; selain itu flag inline. */
    private RegexNode numericOrFlags(int start, int marker) {
        boolean signedDigit = (marker == '+' || marker == '-') && ParseCursor.isDigit(in.peek(1));
        if (marker == 'R' && in.peek(1) == ')') {
            return callUntilClose(start, SpecialKind.RECURSION, Feature.RECURSION);
        }
        if (ParseCursor.isDigit(marker) || signedDigit) {
            boolean recursion = marker == '0' && in.peek(1) == ')';
            return recursion
                    ? callUntilClose(start, SpecialKind.RECURSION, Feature.RECURSION)
                    : callUntilClose(start, SpecialKind.SUBROUTINE_CALL, Feature.SUBROUTINE_CALL);
        }
        return flags.parse(start);
    }

    /** Konstruk tanpa isi pola: baca sampai {@code )}. */
    private RegexNode callUntilClose(int start, SpecialKind kind, Feature feature) {
        while (!in.atEnd() && in.peek() != ')') {
            in.advance(1);
        }
        if (!in.consume(')')) {
            in.problem(new Span(start, start + 1), ProblemCode.UNMATCHED_OPEN_PAREN);
        }
        return special(start, kind, feature);
    }

    /** Konstruk dengan isi (conditional, branch reset, verb): baca sampai kurung seimbang sebagai teks mentah. */
    private RegexNode balancedSpecial(int start, SpecialKind kind, Feature feature) {
        int depth = 1;
        while (!in.atEnd() && depth > 0) {
            int current = in.peek();
            if (current == '\\') {
                in.advance(2);
                continue;
            }
            depth += current == '(' ? 1 : current == ')' ? -1 : 0;
            in.advance(1);
        }
        if (depth > 0) {
            in.problem(new Span(start, start + 1), ProblemCode.UNMATCHED_OPEN_PAREN);
        }
        return special(start, kind, feature);
    }

    private RegexNode special(int start, SpecialKind kind, Feature feature) {
        Span span = in.spanFrom(start);
        if (!dialect.is(RegexFlavor.PHP)) {
            in.unsupported(span, feature);
        }
        return new RegexNode.SpecialConstruct(span, kind, in.textFrom(start));
    }

    private RegexNode invalidSyntax(int start) {
        int problemStart = in.position();
        in.advance(1);
        in.problem(
                new Span(start, in.position()),
                ProblemCode.INVALID_GROUP_SYNTAX,
                in.pattern().substring(problemStart, in.position()));
        return body(start, GroupKind.NON_CAPTURING, 0, "", "", "");
    }

    /** Isi grup sampai {@code )}. Mode extended dikembalikan saat grup selesai (flag inline berlaku per grup). */
    RegexNode body(int start, GroupKind kind, int number, String name, String flagsOn, String flagsOff) {
        boolean outerExtended = parser.extended();
        if (kind == GroupKind.FLAG_GROUP) {
            parser.setExtended(InlineFlagParser.extendedAfter(outerExtended, flagsOn, flagsOff));
        }
        parser.enterGroup();
        RegexNode content = parser.parseAlternation();
        parser.exitGroup();
        if (!in.consume(')')) {
            in.problem(new Span(start, start + 1), ProblemCode.UNMATCHED_OPEN_PAREN);
        }
        parser.setExtended(outerExtended);
        Span span = in.spanFrom(start);
        if (number > 0) {
            parser.registerGroup(number, name, span);
        }
        return new RegexNode.Group(span, kind, number, name, flagsOn, flagsOff, content);
    }
}
