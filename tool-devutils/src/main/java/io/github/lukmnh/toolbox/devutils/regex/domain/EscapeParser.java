package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.Optional;

/**
 * Escape {@code \\x} di luar dan di dalam character class, dengan aturan per flavor. Selalu maju minimal satu karakter
 * dan tidak pernah melempar exception; escape yang salah menjadi node + {@link RegexProblem}.
 */
final class EscapeParser {

    private static final int BELL = 0x07;
    private static final int BACKSPACE = 0x08;
    private static final int TAB = 0x09;
    private static final int NEWLINE = 0x0A;
    private static final int VERTICAL_TAB = 0x0B;
    private static final int FORM_FEED = 0x0C;
    private static final int CARRIAGE_RETURN = 0x0D;
    private static final int ESCAPE = 0x1B;
    private static final int MAX_CODE_POINT = 0x10FFFF;
    private static final int CONTROL_XOR = 0x40;
    private static final int CONTROL_MOD = 32;

    private final ParseCursor in;
    private final ParserDialect dialect;
    private final GroupState groups;
    private final BackreferenceParser references;

    EscapeParser(ParseCursor in, ParserDialect dialect, GroupState groups) {
        this.in = in;
        this.dialect = dialect;
        this.groups = groups;
        this.references = new BackreferenceParser(in, dialect, groups);
    }

    /**
     * Parse escape di posisi {@code \\}.
     *
     * @return kosong bila escape tidak menghasilkan node (PCRE {@code \\E} tanpa {@code \\Q})
     */
    Optional<RegexNode> parse(boolean inClass) {
        int start = in.position();
        in.advance(1);
        if (in.atEnd()) {
            in.problem(in.spanFrom(start), ProblemCode.TRAILING_BACKSLASH);
            return Optional.of(new RegexNode.Unknown(in.spanFrom(start), "\\"));
        }
        int letter = in.peek();
        if (ParseCursor.isDigit(letter)) {
            return Optional.of(inClass ? classDigits(start) : references.digits(start));
        }
        return escapeLetter(start, letter, inClass);
    }

    private Optional<RegexNode> escapeLetter(int start, int letter, boolean inClass) {
        return switch (letter) {
            case 'd', 'D', 'w', 'W', 's', 'S' -> Optional.of(basicShorthand(start, letter));
            case 'h', 'H', 'v', 'V', 'R', 'X', 'N' -> Optional.of(extendedShorthand(start, letter, inClass));
            case 'b', 'B', 'A', 'z', 'Z', 'G' -> Optional.of(anchor(start, letter, inClass));
            case 'p', 'P' -> Optional.of(property(start, letter == 'P'));
            case 'x' -> Optional.of(hex(start));
            case 'u' -> Optional.of(unicode(start));
            case 'c' -> Optional.of(control(start));
            case 'k', 'g' -> Optional.of(namedReference(start, letter, inClass));
            case 'E' -> endQuote(start);
            default -> Optional.of(simple(start, letter, inClass));
        };
    }

    private RegexNode simple(int start, int letter, boolean inClass) {
        return switch (letter) {
            case 't' -> literal(start, TAB);
            case 'n' -> literal(start, NEWLINE);
            case 'r' -> literal(start, CARRIAGE_RETURN);
            case 'f' -> literal(start, FORM_FEED);
            case 'a' -> flavorLiteral(start, BELL, !dialect.is(RegexFlavor.JAVASCRIPT), inClass);
            case 'e' ->
                flavorLiteral(start, ESCAPE, dialect.is(RegexFlavor.JAVA) || dialect.is(RegexFlavor.PHP), inClass);
            case 'o' -> octalBraces(start, inClass);
            case 'K', 'C', 'Q' -> special(start, letter, inClass);
            default -> identity(start, inClass);
        };
    }

    private RegexNode literal(int start, int codePoint) {
        in.advance(1);
        return new RegexNode.Literal(in.spanFrom(start), codePoint);
    }

    /** Escape yang hanya ada di sebagian flavor; di flavor lain diperlakukan seperti identity escape. */
    private RegexNode flavorLiteral(int start, int codePoint, boolean supported, boolean inClass) {
        return supported ? literal(start, codePoint) : identity(start, inClass);
    }

    /** {@code \\.}, {@code \\*}, {@code \\é}: karakter itu sendiri, atau error untuk huruf/digit tak dikenal. */
    private RegexNode identity(int start, boolean inClass) {
        int codePoint = in.nextCodePoint();
        Span span = in.spanFrom(start);
        boolean alphanumeric = codePoint < 128 && Character.isLetterOrDigit(codePoint);
        boolean invalid = dialect.is(RegexFlavor.JAVASCRIPT)
                ? dialect.unicodeMode() && !dialect.jsUnicodeIdentityAllowed(codePoint, inClass)
                : alphanumeric || (dialect.is(RegexFlavor.GO) && codePoint >= 128);
        if (invalid) {
            in.problem(span, ProblemCode.INVALID_ESCAPE, in.textFrom(start));
        }
        return new RegexNode.Literal(span, codePoint);
    }

    private RegexNode basicShorthand(int start, int letter) {
        in.advance(1);
        ShorthandKind kind = switch (letter) {
            case 'd' -> ShorthandKind.DIGIT;
            case 'D' -> ShorthandKind.NOT_DIGIT;
            case 'w' -> ShorthandKind.WORD;
            case 'W' -> ShorthandKind.NOT_WORD;
            case 's' -> ShorthandKind.SPACE;
            default -> ShorthandKind.NOT_SPACE;
        };
        return new RegexNode.Shorthand(in.spanFrom(start), kind);
    }

    private RegexNode extendedShorthand(int start, int letter, boolean inClass) {
        boolean javaOrPhp = dialect.is(RegexFlavor.JAVA) || dialect.is(RegexFlavor.PHP);
        if (letter == 'v' && !javaOrPhp) {
            return literal(start, VERTICAL_TAB);
        }
        if (letter == 'N') {
            return dialect.annexB() ? identity(start, inClass) : notNewline(start, inClass);
        }
        if ((letter == 'R' || letter == 'X') && inClass) {
            return invalid(start);
        }
        in.advance(1);
        Span span = in.spanFrom(start);
        if (!javaOrPhp) {
            in.unsupported(span, extendedFeature(letter));
        }
        return new RegexNode.Shorthand(span, extendedKind(letter));
    }

    private static Feature extendedFeature(int letter) {
        return switch (letter) {
            case 'R' -> Feature.LINEBREAK;
            case 'X' -> Feature.GRAPHEME;
            default -> Feature.HORIZONTAL_VERTICAL_SPACE;
        };
    }

    private static ShorthandKind extendedKind(int letter) {
        return switch (letter) {
            case 'h' -> ShorthandKind.HORIZONTAL_SPACE;
            case 'H' -> ShorthandKind.NOT_HORIZONTAL_SPACE;
            case 'v' -> ShorthandKind.VERTICAL_SPACE;
            case 'V' -> ShorthandKind.NOT_VERTICAL_SPACE;
            case 'R' -> ShorthandKind.LINEBREAK;
            default -> ShorthandKind.GRAPHEME;
        };
    }

    /** PCRE {@code \\N} = selain newline; Java {@code \\N{NAMA KARAKTER}}. */
    private RegexNode notNewline(int start, boolean inClass) {
        if (dialect.is(RegexFlavor.JAVA)) {
            return namedCharacter(start);
        }
        if (!dialect.is(RegexFlavor.PHP) || inClass) {
            return invalid(start);
        }
        in.advance(1);
        return new RegexNode.Shorthand(in.spanFrom(start), ShorthandKind.NOT_NEWLINE);
    }

    private RegexNode namedCharacter(int start) {
        in.advance(1);
        if (!in.consume('{')) {
            in.problem(in.spanFrom(start), ProblemCode.INVALID_ESCAPE, in.textFrom(start));
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        int nameStart = in.position();
        while (!in.atEnd() && in.peek() != '}') {
            in.advance(1);
        }
        String name = in.pattern().substring(nameStart, in.position());
        boolean closed = in.consume('}');
        int codePoint = codePointOf(name);
        if (!closed || codePoint < 0) {
            in.problem(in.spanFrom(start), ProblemCode.INVALID_ESCAPE, in.textFrom(start));
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        return new RegexNode.Literal(in.spanFrom(start), codePoint);
    }

    private static int codePointOf(String name) {
        try {
            return Character.codePointOf(name);
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    private RegexNode anchor(int start, int letter, boolean inClass) {
        if (inClass) {
            return letter == 'b' ? literal(start, BACKSPACE) : identityOrInvalid(start);
        }
        if (letter == 'b' && dialect.is(RegexFlavor.JAVA) && in.lookingAt("b{g}")) {
            // Batas grapheme Java: sah tapi tidak punya node khusus, tampil sebagai konstruk lanjutan
            in.advance("b{g}".length());
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        if (dialect.annexB() && letter != 'b' && letter != 'B') {
            in.unsupported(new Span(start, start + 2), anchorFeature(letter));
        }
        in.advance(1);
        Span span = in.spanFrom(start);
        reportAnchorSupport(span, letter);
        return new RegexNode.Anchor(span, anchorKind(letter));
    }

    private void reportAnchorSupport(Span span, int letter) {
        boolean js = dialect.is(RegexFlavor.JAVASCRIPT);
        boolean go = dialect.is(RegexFlavor.GO);
        boolean unsupported = switch (letter) {
            case 'A', 'z' -> js && dialect.unicodeMode();
            case 'Z', 'G' -> (js && dialect.unicodeMode()) || go;
            default -> false;
        };
        if (unsupported) {
            in.unsupported(span, anchorFeature(letter));
        }
    }

    private static Feature anchorFeature(int letter) {
        return switch (letter) {
            case 'Z' -> Feature.END_BEFORE_NEWLINE_ANCHOR;
            case 'G' -> Feature.PREVIOUS_MATCH_END_ANCHOR;
            default -> Feature.INPUT_ANCHOR;
        };
    }

    private static AnchorKind anchorKind(int letter) {
        return switch (letter) {
            case 'b' -> AnchorKind.WORD_BOUNDARY;
            case 'B' -> AnchorKind.NON_WORD_BOUNDARY;
            case 'A' -> AnchorKind.INPUT_START;
            case 'z' -> AnchorKind.INPUT_END;
            case 'Z' -> AnchorKind.INPUT_END_BEFORE_NEWLINE;
            default -> AnchorKind.PREVIOUS_MATCH_END;
        };
    }

    /** Di dalam class: JS Annex B menerima huruf apa pun sebagai literal, flavor lain error. */
    private RegexNode identityOrInvalid(int start) {
        return dialect.annexB() ? identity(start, true) : invalid(start);
    }

    private RegexNode invalid(int start) {
        int codePoint = in.nextCodePoint();
        Span span = in.spanFrom(start);
        in.problem(span, ProblemCode.INVALID_ESCAPE, in.textFrom(start));
        return new RegexNode.Literal(span, codePoint);
    }

    private RegexNode property(int start, boolean negatedLetter) {
        if (dialect.is(RegexFlavor.JAVASCRIPT) && !dialect.unicodeMode()) {
            return identity(start, false);
        }
        in.advance(1);
        if (!in.consume('{')) {
            return singleLetterProperty(start, negatedLetter);
        }
        int nameStart = in.position();
        while (!in.atEnd() && in.peek() != '}') {
            in.advance(1);
        }
        String name = in.pattern().substring(nameStart, in.position());
        if (!in.consume('}') || name.isEmpty()) {
            in.problem(in.spanFrom(start), ProblemCode.INVALID_ESCAPE, in.textFrom(start));
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        boolean caretNegation = dialect.is(RegexFlavor.PHP) && name.startsWith("^");
        String cleanName = caretNegation ? name.substring(1) : name;
        return new RegexNode.UnicodeProperty(in.spanFrom(start), cleanName, negatedLetter ^ caretNegation);
    }

    /** {@code \\pL}: hanya Java, Go, PHP. */
    private RegexNode singleLetterProperty(int start, boolean negated) {
        if (dialect.is(RegexFlavor.JAVASCRIPT) || in.atEnd() || !ParseCursor.isAsciiLetter(in.peek())) {
            in.problem(in.spanFrom(start), ProblemCode.INVALID_ESCAPE, in.textFrom(start));
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        String name = String.valueOf((char) in.peek());
        in.advance(1);
        return new RegexNode.UnicodeProperty(in.spanFrom(start), name, negated);
    }

    private RegexNode hex(int start) {
        in.advance(1);
        if (in.peek() == '{' && !dialect.is(RegexFlavor.JAVASCRIPT)) {
            return bracedCodePoint(start, 16);
        }
        boolean php = dialect.is(RegexFlavor.PHP);
        int value = in.readRadix(16, php ? 0 : 2, 2);
        if (php && value < 0) {
            value = 0;
        }
        if (value < 0) {
            return failedEscape(start, 'x');
        }
        return new RegexNode.Literal(in.spanFrom(start), value);
    }

    /** {@code \\x{1F600}} / {@code \\o{17}} / {@code \\u{1F600}}. */
    private RegexNode bracedCodePoint(int start, int radix) {
        in.advance(1);
        int value = in.readRadix(radix, 1, 8);
        boolean closed = in.consume('}');
        if (value < 0 || !closed || value > MAX_CODE_POINT) {
            in.problem(in.spanFrom(start), ProblemCode.INVALID_UNICODE_ESCAPE, in.textFrom(start));
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        return new RegexNode.Literal(in.spanFrom(start), value);
    }

    /** JS Annex B: escape tidak lengkap menjadi huruf itu sendiri; flavor/mode lain error. */
    private RegexNode failedEscape(int start, int letter) {
        Span span = in.spanFrom(start);
        if (!dialect.annexB()) {
            in.problem(span, ProblemCode.INVALID_ESCAPE, in.textFrom(start));
        }
        return new RegexNode.Literal(span, letter);
    }

    private RegexNode unicode(int start) {
        boolean supported = dialect.is(RegexFlavor.JAVA) || dialect.is(RegexFlavor.JAVASCRIPT);
        if (!supported) {
            return invalid(start);
        }
        in.advance(1);
        if (in.peek() == '{' && dialect.unicodeMode()) {
            return bracedCodePoint(start, 16);
        }
        int value = in.readRadix(16, 4, 4);
        if (value < 0) {
            if (dialect.annexB()) {
                return new RegexNode.Literal(in.spanFrom(start), 'u');
            }
            in.problem(in.spanFrom(start), ProblemCode.INVALID_UNICODE_ESCAPE, in.textFrom(start));
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        return new RegexNode.Literal(in.spanFrom(start), combineSurrogate(value));
    }

    /** {@code \\uD83D\\uDE00} digabung menjadi satu code point (Java, JS mode Unicode). */
    private int combineSurrogate(int high) {
        boolean pairs = dialect.is(RegexFlavor.JAVA) || dialect.unicodeMode();
        if (!pairs || !Character.isHighSurrogate((char) high) || !in.lookingAt("\\u")) {
            return high;
        }
        int resume = in.position();
        in.advance(2);
        int low = in.readRadix(16, 4, 4);
        if (low < 0 || !Character.isLowSurrogate((char) low)) {
            in.reset(resume);
            return high;
        }
        return Character.toCodePoint((char) high, (char) low);
    }

    private RegexNode control(int start) {
        in.advance(1);
        int target = in.peek();
        if (dialect.is(RegexFlavor.GO) || target == ParseCursor.END) {
            in.problem(in.spanFrom(start), ProblemCode.INVALID_ESCAPE, in.textFrom(start));
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        if (dialect.is(RegexFlavor.JAVASCRIPT) && !ParseCursor.isAsciiLetter(target)) {
            return jsInvalidControl(start);
        }
        in.advance(1);
        int value =
                dialect.is(RegexFlavor.JAVASCRIPT) ? target % CONTROL_MOD : Character.toUpperCase(target) ^ CONTROL_XOR;
        return new RegexNode.Literal(in.spanFrom(start), value);
    }

    /** JS Annex B: {@code \\c} tanpa huruf = backslash literal, huruf {@code c} dibaca ulang sebagai literal. */
    private RegexNode jsInvalidControl(int start) {
        if (dialect.unicodeMode()) {
            in.problem(in.spanFrom(start), ProblemCode.INVALID_ESCAPE, in.textFrom(start));
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        in.reset(start + 1);
        return new RegexNode.Literal(new Span(start, start + 1), '\\');
    }

    private RegexNode octalBraces(int start, boolean inClass) {
        if (!dialect.is(RegexFlavor.PHP) || in.peek(1) != '{') {
            return identity(start, inClass);
        }
        in.advance(1);
        return bracedCodePoint(start, 8);
    }

    private RegexNode special(int start, int letter, boolean inClass) {
        if (letter == 'Q') {
            return quote(start, inClass);
        }
        boolean php = dialect.is(RegexFlavor.PHP);
        if (inClass || dialect.annexB()) {
            return identityOrInvalid(start);
        }
        if (letter == 'C' && !php) {
            return invalid(start);
        }
        in.advance(1);
        Span span = in.spanFrom(start);
        if (!php) {
            in.unsupported(span, Feature.KEEP_OUT);
        }
        SpecialKind kind = letter == 'K' ? SpecialKind.KEEP_OUT : SpecialKind.SINGLE_BYTE;
        return new RegexNode.SpecialConstruct(span, kind, in.textFrom(start));
    }

    /** {@code \\Q...\\E} sampai {@code \\E} atau akhir pola. */
    private RegexNode quote(int start, boolean inClass) {
        if (dialect.annexB()) {
            return identity(start, inClass);
        }
        in.advance(1);
        int textStart = in.position();
        int end = in.pattern().indexOf("\\E", textStart);
        int textEnd = end < 0 ? in.pattern().length() : end;
        in.reset(end < 0 ? textEnd : textEnd + 2);
        Span span = in.spanFrom(start);
        if (dialect.is(RegexFlavor.JAVASCRIPT)) {
            in.unsupported(span, Feature.QUOTE);
        }
        return new RegexNode.QuotedLiteral(span, in.pattern().substring(textStart, textEnd));
    }

    /** PCRE mengabaikan {@code \\E} tanpa {@code \\Q}; Java dan Go menolaknya. */
    private Optional<RegexNode> endQuote(int start) {
        if (dialect.is(RegexFlavor.PHP)) {
            in.advance(1);
            return Optional.empty();
        }
        return Optional.of(dialect.annexB() ? identity(start, false) : invalid(start));
    }

    private RegexNode namedReference(int start, int letter, boolean inClass) {
        if (inClass) {
            return identityOrInvalid(start);
        }
        return letter == 'k' ? references.kReference(start) : references.gReference(start);
    }

    /** Escape angka di dalam class: selalu karakter (octal), tidak pernah backreference. */
    private RegexNode classDigits(int start) {
        int first = in.peek();
        boolean octalAllowed = !dialect.is(RegexFlavor.JAVA) && !dialect.unicodeMode();
        if (first == '0' || (ParseCursor.isOctal(first) && octalAllowed)) {
            return references.octalLiteral(start);
        }
        if (dialect.annexB()) {
            in.advance(1);
            return new RegexNode.Literal(in.spanFrom(start), first);
        }
        return invalid(start);
    }
}
