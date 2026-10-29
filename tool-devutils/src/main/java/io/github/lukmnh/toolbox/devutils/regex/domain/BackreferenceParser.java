package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.Optional;

/**
 * Escape angka dan referensi grup: {@code \\1}, {@code \\k<n>}, PCRE {@code \\g{-1}}. Arti angka berbeda per flavor
 * (backreference, octal, atau literal), jadi aturannya dikumpulkan di sini.
 */
final class BackreferenceParser {

    private static final int SINGLE_DIGIT_LIMIT = 10;
    private static final int MAX_OCTAL = 0377;

    private final ParseCursor in;
    private final ParserDialect dialect;
    private final GroupState groups;

    BackreferenceParser(ParseCursor in, ParserDialect dialect, GroupState groups) {
        this.in = in;
        this.dialect = dialect;
        this.groups = groups;
    }

    /** Escape angka di luar class. Posisi di digit pertama (setelah {@code \\}). */
    RegexNode digits(int start) {
        return switch (dialect.flavor()) {
            case JAVA -> javaDigits(start);
            case GO -> goDigits(start);
            case JAVASCRIPT -> jsDigits(start);
            case PHP -> phpDigits(start);
        };
    }

    /** Java: digit pertama selalu backreference; digit berikut ikut selama nomornya ≤ grup yang sudah dibuka. */
    private RegexNode javaDigits(int start) {
        if (in.peek() == '0') {
            return octalLiteral(start);
        }
        int number = in.peek() - '0';
        in.advance(1);
        while (ParseCursor.isDigit(in.peek()) && number * 10 + (in.peek() - '0') <= groups.openedSoFar()) {
            number = number * 10 + (in.peek() - '0');
            in.advance(1);
        }
        return new RegexNode.Backreference(in.spanFrom(start), number, "");
    }

    /** Go: octal ({@code \\0}, {@code \\123}); satu digit 1–9 = backreference yang tidak didukung RE2. */
    private RegexNode goDigits(int start) {
        int first = in.peek();
        boolean octal = first == '0' || (ParseCursor.isOctal(first) && ParseCursor.isOctal(in.peek(1)));
        if (octal) {
            return octalLiteral(start);
        }
        in.advance(1);
        Span span = in.spanFrom(start);
        in.unsupported(span, Feature.BACKREFERENCE);
        return new RegexNode.Backreference(span, first - '0', "");
    }

    /** JavaScript: angka ≤ jumlah grup di seluruh pola = backreference; selain itu octal/literal (Annex B). */
    private RegexNode jsDigits(int start) {
        if (in.peek() == '0' && !ParseCursor.isDigit(in.peek(1))) {
            in.advance(1);
            return new RegexNode.Literal(in.spanFrom(start), 0);
        }
        int resume = in.position();
        int number = in.readNumber(Integer.MAX_VALUE);
        if (number > 0 && number <= groups.total() && in.pattern().charAt(resume) != '0') {
            return new RegexNode.Backreference(in.spanFrom(start), number, "");
        }
        in.reset(resume);
        if (dialect.unicodeMode()) {
            in.readNumber(Integer.MAX_VALUE);
            in.problem(in.spanFrom(start), ProblemCode.INVALID_ESCAPE, in.textFrom(start));
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        return legacyDigit(start);
    }

    /** Annex B: {@code \\8}/{@code \\9} = angka literal, selain itu octal. */
    private RegexNode legacyDigit(int start) {
        int first = in.peek();
        if (first == '8' || first == '9') {
            in.advance(1);
            return new RegexNode.Literal(in.spanFrom(start), first);
        }
        return octalLiteral(start);
    }

    /**
     * PCRE: angka &lt; 10, diawali 8/9, atau ≤ jumlah grup sebelumnya = backreference; selain itu octal
     * ({@code \\11} tanpa 11 grup = tab).
     */
    private RegexNode phpDigits(int start) {
        if (in.peek() == '0') {
            return octalLiteral(start);
        }
        int resume = in.position();
        int first = in.peek();
        int number = in.readNumber(Integer.MAX_VALUE);
        boolean reference =
                number < SINGLE_DIGIT_LIMIT || first == '8' || first == '9' || number <= groups.openedSoFar();
        if (reference) {
            return new RegexNode.Backreference(in.spanFrom(start), number, "");
        }
        in.reset(resume);
        return octalLiteral(start);
    }

    /** Octal maks 0377. Java: {@code \\0} wajib diikuti 1–3 digit octal; flavor lain maks 3 digit total. */
    RegexNode octalLiteral(int start) {
        int maxValue = MAX_OCTAL;
        boolean javaLeadingZero = dialect.is(RegexFlavor.JAVA);
        if (javaLeadingZero) {
            in.advance(1);
        }
        int value = 0;
        int digits = 0;
        while (digits < 3 && ParseCursor.isOctal(in.peek()) && value * 8 + (in.peek() - '0') <= maxValue) {
            value = value * 8 + (in.peek() - '0');
            in.advance(1);
            digits++;
        }
        if (javaLeadingZero && digits == 0) {
            in.problem(in.spanFrom(start), ProblemCode.INVALID_ESCAPE, in.textFrom(start));
        }
        return new RegexNode.Literal(in.spanFrom(start), value);
    }

    /** {@code \\k<nama>} (Java, JS, PHP), {@code \\k'nama'} dan {@code \\k{nama}} (PHP). */
    RegexNode kReference(int start) {
        in.advance(1);
        boolean jsLiteralK = dialect.annexB() && !groups.hasNamedGroups();
        if (jsLiteralK) {
            return new RegexNode.Literal(in.spanFrom(start), 'k');
        }
        if (dialect.is(RegexFlavor.GO)) {
            return invalidReference(start);
        }
        char close = closingFor(in.peek());
        boolean allowed = close == '>' || dialect.is(RegexFlavor.PHP);
        if (close == 0 || !allowed) {
            return invalidReference(start);
        }
        in.advance(1);
        return namedUntil(start, close);
    }

    /** PCRE {@code \\g}: backreference ({@code \\g1}, {@code \\g{-1}}, {@code \\g{nama}}) atau subroutine. */
    RegexNode gReference(int start) {
        in.advance(1);
        if (!dialect.is(RegexFlavor.PHP)) {
            return dialect.annexB() ? new RegexNode.Literal(in.spanFrom(start), 'g') : invalidReference(start);
        }
        int next = in.peek();
        if (next == '<' || next == '\'') {
            return subroutine(start, next == '<' ? '>' : '\'');
        }
        if (in.consume('{')) {
            return bracedG(start);
        }
        return signedNumber(start).orElseGet(() -> invalidReference(start));
    }

    private RegexNode subroutine(int start, char close) {
        in.advance(1);
        while (!in.atEnd() && in.peek() != close) {
            in.advance(1);
        }
        in.consume(close);
        return new RegexNode.SpecialConstruct(in.spanFrom(start), SpecialKind.SUBROUTINE_CALL, in.textFrom(start));
    }

    private RegexNode bracedG(int start) {
        int resume = in.position();
        Optional<RegexNode> numeric = signedNumber(start);
        if (numeric.isPresent() && in.consume('}')) {
            return withClosingBrace(numeric.get(), start);
        }
        in.reset(resume);
        return namedUntil(start, '}');
    }

    /** Span dari {@link #signedNumber} dibuat sebelum {@code }} dibaca; perluas supaya hover/highlight utuh. */
    private RegexNode withClosingBrace(RegexNode node, int start) {
        Span span = in.spanFrom(start);
        return switch (node) {
            case RegexNode.Backreference reference ->
                new RegexNode.Backreference(span, reference.number(), reference.name());
            case RegexNode.Unknown unknown -> new RegexNode.Unknown(span, in.textFrom(start));
            default -> node;
        };
    }

    /** {@code 1}, {@code -1}: nomor relatif dihitung dari grup yang sudah dibuka. */
    private Optional<RegexNode> signedNumber(int start) {
        boolean negative = in.consume('-');
        int number = in.readNumber(Integer.MAX_VALUE);
        if (number < 0) {
            return Optional.empty();
        }
        int absolute = negative ? groups.openedSoFar() - number + 1 : number;
        Span span = in.spanFrom(start);
        if (absolute < 1) {
            in.problem(span, ProblemCode.UNKNOWN_GROUP_REFERENCE, in.textFrom(start));
            return Optional.of(new RegexNode.Unknown(span, in.textFrom(start)));
        }
        return Optional.of(new RegexNode.Backreference(span, absolute, ""));
    }

    /** Nama sampai {@code close}; nama divalidasi keberadaannya setelah seluruh pola di-parse. */
    RegexNode namedUntil(int start, char close) {
        int nameStart = in.position();
        while (!in.atEnd() && in.peek() != close && in.peek() != ')') {
            in.advance(1);
        }
        String name = in.pattern().substring(nameStart, in.position());
        if (!in.consume(close) || name.isEmpty()) {
            in.problem(in.spanFrom(start), ProblemCode.INVALID_GROUP_NAME, name);
            return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
        }
        return new RegexNode.Backreference(in.spanFrom(start), 0, name);
    }

    private RegexNode invalidReference(int start) {
        in.problem(in.spanFrom(start), ProblemCode.INVALID_ESCAPE, in.textFrom(start));
        return new RegexNode.Unknown(in.spanFrom(start), in.textFrom(start));
    }

    private static char closingFor(int open) {
        return switch (open) {
            case '<' -> '>';
            case '\'' -> '\'';
            case '{' -> '}';
            default -> 0;
        };
    }
}
