package io.github.lukmnh.toolbox.devutils.regex.domain;

/** Flag inline {@code (?i)}, {@code (?i-s)}, dan grup flag {@code (?i:...)}. */
final class InlineFlagParser {

    private final ParseCursor in;
    private final ParserDialect dialect;
    private final PatternParser parser;
    private final GroupParser groups;

    InlineFlagParser(ParseCursor in, ParserDialect dialect, PatternParser parser, GroupParser groups) {
        this.in = in;
        this.dialect = dialect;
        this.parser = parser;
        this.groups = groups;
    }

    /** Posisi setelah {@code (?}. */
    RegexNode parse(int start) {
        String on = readLetters();
        String off = in.consume('-') ? readLetters() : "";
        if (in.consume(':')) {
            return groups.body(start, GroupKind.FLAG_GROUP, 0, "", on, off);
        }
        if (in.consume(')') && !(on.isEmpty() && off.isEmpty())) {
            return standalone(start, on, off);
        }
        return invalid(start);
    }

    private RegexNode standalone(int start, String on, String off) {
        Span span = in.spanFrom(start);
        if (dialect.is(RegexFlavor.JAVASCRIPT)) {
            in.unsupported(span, Feature.INLINE_FLAGS);
        }
        // Flag inline berlaku sampai akhir grup yang sedang dibuka; GroupParser.body mengembalikan mode semula
        parser.setExtended(extendedAfter(parser.extended(), on, off));
        return new RegexNode.InlineFlags(span, on, off);
    }

    private String readLetters() {
        int start = in.position();
        while (!in.atEnd() && (Character.isLetter(in.peek()) || in.peek() == '^')) {
            int letterStart = in.position();
            int letter = in.peek();
            in.advance(1);
            if (dialect.inlineFlagLetters().indexOf(letter) < 0) {
                in.problem(in.spanFrom(letterStart), ProblemCode.UNKNOWN_FLAG, String.valueOf((char) letter));
            }
        }
        return in.pattern().substring(start, in.position());
    }

    /** {@code (?...} yang tidak dikenali: catat, lalu lanjutkan sebagai grup non-capture supaya parse tidak putus. */
    private RegexNode invalid(int start) {
        int textStart = start + 2;
        if (!in.atEnd()) {
            in.advance(1);
        }
        String text = in.pattern().substring(Math.min(textStart, in.position()), in.position());
        in.problem(in.spanFrom(start), ProblemCode.INVALID_GROUP_SYNTAX, text);
        if (in.pattern().charAt(in.position() - 1) == ')') {
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        return groups.body(start, GroupKind.NON_CAPTURING, 0, "", "", "");
    }

    static boolean extendedAfter(boolean current, String on, String off) {
        if (off.indexOf('x') >= 0) {
            return false;
        }
        return current || on.indexOf('x') >= 0;
    }
}
