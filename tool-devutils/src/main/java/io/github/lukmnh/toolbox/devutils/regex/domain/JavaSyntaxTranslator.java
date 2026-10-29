package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Menerjemahkan pohon pola JavaScript/PHP ke sintaks {@code java.util.regex} dengan koreksi semantik (dot, anchor,
 * {@code \\s}, nama grup, flag PCRE). Fitur tanpa padanan dicatat sebagai {@link ProblemCode#UNSUPPORTED_BY_ENGINE}.
 */
final class JavaSyntaxTranslator {

    /** ECMAScript LineTerminator. */
    static final String JS_LINE_TERMINATORS = "\\x{A}\\x{D}\\x{2028}\\x{2029}";

    /** ECMAScript WhiteSpace + LineTerminator (definisi persis {@code \\s} di JavaScript). */
    static final String JS_WHITESPACE = "\\x{9}\\x{A}\\x{B}\\x{C}\\x{D}\\x{20}\\x{A0}\\x{1680}\\x{2000}-\\x{200A}"
            + "\\x{2028}\\x{2029}\\x{202F}\\x{205F}\\x{3000}\\x{FEFF}";

    private static final String JS_DOT = "[^" + JS_LINE_TERMINATORS + "]";
    private static final String ANY = "[\\s\\S]";
    private static final String NEVER = "(?!)";
    private static final String JS_LINE_START = "(?<![^" + JS_LINE_TERMINATORS + "])";
    private static final String JS_LINE_END = "(?![^" + JS_LINE_TERMINATORS + "])";
    private static final String STICKY = "\\G";
    private static final String GROUP_NAME_PREFIX = "n";
    private static final int ASCII_DELETE = 0x7F;
    private static final int ASCII_SPACE = 0x20;

    private final ParseResult parsed;
    private final RegexRequest request;
    private final boolean javaScript;
    private final Map<String, Integer> numbersByName;
    private final TranslationOutput out = new TranslationOutput();
    private final List<RegexProblem> problems = new ArrayList<>();

    private JavaSyntaxTranslator(ParseResult parsed, RegexRequest request) {
        this.parsed = parsed;
        this.request = request;
        this.javaScript = request.flavor() == RegexFlavor.JAVASCRIPT;
        this.numbersByName = new HashMap<>();
        parsed.groups().forEach(group -> numbersByName.putIfAbsent(group.name(), group.number()));
    }

    record Translation(String source, int flags, OffsetMap offsets, List<RegexProblem> problems) {}

    static Translation translate(ParseResult parsed, RegexRequest request) {
        return new JavaSyntaxTranslator(parsed, request).run();
    }

    private Translation run() {
        boolean anchored = request.has(RegexFlag.JS_STICKY) || request.has(RegexFlag.PHP_ANCHORED);
        if (anchored) {
            out.emit(STICKY, 0);
        }
        emit(parsed.root(), initialMode(), false);
        return new Translation(
                out.text(), javaFlags(), out.offsets(parsed.pattern().length()), List.copyOf(problems));
    }

    private Mode initialMode() {
        boolean multiline = request.has(RegexFlag.JS_MULTILINE) || request.has(RegexFlag.PHP_MULTILINE);
        return new Mode(multiline, request.has(RegexFlag.JS_DOT_ALL));
    }

    /** Flag Java. JS: m/s ditangani lewat terjemahan; PHP: newline PCRE default hanya LF ({@code UNIX_LINES}). */
    private int javaFlags() {
        if (javaScript) {
            return request.has(RegexFlag.JS_IGNORE_CASE) ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
        }
        int flags = Pattern.UNIX_LINES;
        flags |= request.has(RegexFlag.PHP_MULTILINE) ? Pattern.MULTILINE : 0;
        flags |= request.has(RegexFlag.PHP_DOTALL) ? Pattern.DOTALL : 0;
        flags |= request.has(RegexFlag.PHP_IGNORE_CASE) ? Pattern.CASE_INSENSITIVE : 0;
        flags |= request.has(RegexFlag.PHP_UTF8) ? Pattern.UNICODE_CHARACTER_CLASS | Pattern.UNICODE_CASE : 0;
        return flags;
    }

    private void emit(RegexNode node, Mode mode, boolean inClass) {
        int origin = node.span().start();
        switch (node) {
            case RegexNode.Alternation alternation -> emitAlternation(alternation, mode);
            case RegexNode.Sequence sequence -> sequence.items().forEach(item -> emit(item, mode, inClass));
            case RegexNode.Group group -> emitGroup(group, mode);
            case RegexNode.Quantified quantified -> emitQuantified(quantified, mode);
            case RegexNode.Literal literal -> out.emit(literal(literal.codePoint()), origin);
            case RegexNode.QuotedLiteral quoted ->
                quoted.text().codePoints().forEach(c -> out.emit(literal(c), origin));
            case RegexNode.AnyChar ignored -> out.emit(javaScript ? (mode.dotAll ? ANY : JS_DOT) : ".", origin);
            case RegexNode.Anchor anchor -> out.emit(anchor(anchor.kind(), mode), origin);
            case RegexNode.Shorthand shorthand -> out.emit(shorthand(shorthand.kind(), inClass), origin);
            case RegexNode.UnicodeProperty property -> emitProperty(property);
            case RegexNode.PosixClass posix -> out.emit(PosixClasses.toJava(posix.name(), posix.negated()), origin);
            case RegexNode.CharClass charClass -> emitClass(charClass, inClass);
            case RegexNode.ClassRange range -> out.emit(range(range), origin);
            case RegexNode.ClassOperator operator -> out.emit("&&", origin);
            case RegexNode.Backreference reference -> out.emit(backreference(reference), origin);
            case RegexNode.InlineFlags flags -> emitInlineFlags(flags, mode);
            case RegexNode.Comment ignored -> {
                // Komentar tidak berpengaruh pada pencocokan
            }
            case RegexNode.SpecialConstruct special ->
                unsupported(special.span(), Feature.valueOf(special.kind().name()));
            case RegexNode.Unknown ignored -> {
                // Bagian rusak sudah dicatat parser; terjemahan tidak akan dijalankan
            }
        }
    }

    private void emitAlternation(RegexNode.Alternation alternation, Mode mode) {
        for (int index = 0; index < alternation.branches().size(); index++) {
            RegexNode branch = alternation.branches().get(index);
            if (index > 0) {
                out.emit("|", branch.span().start());
            }
            // PCRE: perubahan flag inline di satu cabang ikut berlaku di cabang berikutnya
            emit(branch, mode, false);
        }
    }

    private void emitGroup(RegexNode.Group group, Mode mode) {
        int origin = group.span().start();
        Mode inner = mode.copy();
        String open = switch (group.kind()) {
            case CAPTURING -> "(";
            case NAMED -> "(?<" + GROUP_NAME_PREFIX + group.number() + ">";
            case NON_CAPTURING -> "(?:";
            case ATOMIC -> "(?>";
            case LOOKAHEAD -> "(?=";
            case NEGATIVE_LOOKAHEAD -> "(?!";
            case LOOKBEHIND -> lookbehind(group, "(?<=");
            case NEGATIVE_LOOKBEHIND -> lookbehind(group, "(?<!");
            case FLAG_GROUP -> flagGroupOpen(group, inner);
        };
        out.emit(open, origin);
        emit(group.body(), inner, false);
        out.emit(")", group.span().end() - 1);
    }

    /** Java hanya menerima lookbehind dengan panjang maksimum yang jelas. */
    private String lookbehind(RegexNode.Group group, String open) {
        if (UnboundedCheck.containsUnbounded(group.body())) {
            unsupported(group.span(), Feature.UNBOUNDED_LOOKBEHIND);
        }
        return open;
    }

    private String flagGroupOpen(RegexNode.Group group, Mode inner) {
        String on = javaFlagLetters(group.flagsOn(), group.span(), inner, true);
        String off = javaFlagLetters(group.flagsOff(), group.span(), inner, false);
        if (on.isEmpty() && off.isEmpty()) {
            return "(?:";
        }
        return "(?" + on + (off.isEmpty() ? "" : "-" + off) + ":";
    }

    private void emitInlineFlags(RegexNode.InlineFlags flags, Mode mode) {
        String on = javaFlagLetters(flags.flagsOn(), flags.span(), mode, true);
        String off = javaFlagLetters(flags.flagsOff(), flags.span(), mode, false);
        if (!on.isEmpty() || !off.isEmpty()) {
            out.emit(
                    "(?" + on + (off.isEmpty() ? "" : "-" + off) + ")",
                    flags.span().start());
        }
    }

    /**
     * Ubah huruf flag JS/PCRE menjadi flag inline Java sambil memperbarui {@code mode}. JS m/s ditangani lewat
     * terjemahan anchor/dot, jadi hanya i yang diteruskan (sebagai {@code iu} supaya case folding Unicode).
     */
    private String javaFlagLetters(String letters, Span span, Mode mode, boolean enable) {
        StringBuilder java = new StringBuilder();
        for (char letter : letters.toCharArray()) {
            switch (letter) {
                case 'i' -> java.append(javaScript ? "iu" : "i");
                case 'm' -> {
                    mode.multiline = enable;
                    java.append(javaScript ? "" : "m");
                }
                case 's' -> {
                    mode.dotAll = enable;
                    java.append(javaScript ? "" : "s");
                }
                case 'x' -> {
                    // Spasi dan komentar sudah dibuang parser
                }
                default -> unsupported(span, Feature.INLINE_FLAGS);
            }
        }
        return java.toString();
    }

    private void emitQuantified(RegexNode.Quantified quantified, Mode mode) {
        boolean wrap = quantified.target() instanceof RegexNode.QuotedLiteral quoted
                && quoted.text().codePointCount(0, quoted.text().length()) != 1;
        int origin = quantified.span().start();
        if (wrap) {
            out.emit("(?:", origin);
        }
        emit(quantified.target(), mode, false);
        if (wrap) {
            out.emit(")", origin);
        }
        out.emit(quantifier(quantified), quantified.target().span().end());
    }

    private String quantifier(RegexNode.Quantified quantified) {
        int min = quantified.min();
        int max = quantified.max();
        boolean unbounded = max == RegexNode.Quantified.UNBOUNDED;
        String base;
        if (min == 0 && unbounded) {
            base = "*";
        } else if (min == 1 && unbounded) {
            base = "+";
        } else if (min == 0 && max == 1) {
            base = "?";
        } else if (unbounded) {
            base = "{" + min + ",}";
        } else {
            base = min == max ? "{" + min + "}" : "{" + min + "," + max + "}";
        }
        // Jumlah pasti ({n}) tidak punya beda rakus/malas, jadi modifier U tidak perlu menambah sufiks
        return min == max ? base + possessiveSuffix(quantified.mode()) : base + modeSuffix(quantified.mode());
    }

    private static String possessiveSuffix(QuantifierMode mode) {
        return switch (mode) {
            case GREEDY -> "";
            case LAZY -> "?";
            case POSSESSIVE -> "+";
        };
    }

    /** PCRE modifier U membalik rakus ↔ malas; posesif tidak terpengaruh. */
    private String modeSuffix(QuantifierMode mode) {
        boolean ungreedy = request.has(RegexFlag.PHP_UNGREEDY);
        return switch (mode) {
            case GREEDY -> ungreedy ? "?" : "";
            case LAZY -> ungreedy ? "" : "?";
            case POSSESSIVE -> "+";
        };
    }

    private String anchor(AnchorKind kind, Mode mode) {
        return switch (kind) {
            case LINE_START -> javaScript && mode.multiline ? JS_LINE_START : "^";
            case LINE_END -> lineEnd(mode);
            case WORD_BOUNDARY -> "\\b";
            case NON_WORD_BOUNDARY -> "\\B";
            case INPUT_START -> "\\A";
            case INPUT_END -> "\\z";
            case INPUT_END_BEFORE_NEWLINE -> "\\Z";
            case PREVIOUS_MATCH_END -> "\\G";
        };
    }

    /** JS tanpa m: hanya akhir input. PHP modifier D (tanpa m): idem; selain itu {@code $} Java + UNIX_LINES. */
    private String lineEnd(Mode mode) {
        if (javaScript) {
            return mode.multiline ? JS_LINE_END : "\\z";
        }
        return request.has(RegexFlag.PHP_DOLLAR_END_ONLY) && !mode.multiline ? "\\z" : "$";
    }

    private String shorthand(ShorthandKind kind, boolean inClass) {
        return switch (kind) {
            case DIGIT -> "\\d";
            case NOT_DIGIT -> "\\D";
            case WORD -> "\\w";
            case NOT_WORD -> "\\W";
            case SPACE -> javaScript ? (inClass ? JS_WHITESPACE : "[" + JS_WHITESPACE + "]") : "\\s";
            case NOT_SPACE -> javaScript ? "[^" + JS_WHITESPACE + "]" : "\\S";
            case HORIZONTAL_SPACE -> "\\h";
            case NOT_HORIZONTAL_SPACE -> "\\H";
            case VERTICAL_SPACE -> "\\v";
            case NOT_VERTICAL_SPACE -> "\\V";
            case LINEBREAK -> "\\R";
            case GRAPHEME -> "\\X";
            case NOT_NEWLINE -> "[^\\n]";
        };
    }

    private void emitProperty(RegexNode.UnicodeProperty property) {
        Optional<String> java = javaScript
                ? UnicodePropertyMapper.javaScript(property.name(), property.negated())
                : UnicodePropertyMapper.php(property.name(), property.negated());
        if (java.isEmpty()) {
            unsupported(property.span(), Feature.UNICODE_PROPERTY);
            return;
        }
        out.emit(java.get(), property.span().start());
    }

    /** JS {@code []} tidak pernah cocok dan {@code [^]} cocok semua; Java menolak keduanya, jadi diganti. */
    private void emitClass(RegexNode.CharClass charClass, boolean nested) {
        int origin = charClass.span().start();
        if (javaScript && !nested && charClass.items().isEmpty()) {
            out.emit(charClass.negated() ? ANY : NEVER, origin);
            return;
        }
        List<List<RegexNode>> operands = new ArrayList<>();
        List<SetOperator> operators = new ArrayList<>();
        splitOperands(charClass.items(), operands, operators);
        boolean wrapOperands = charClass.negated() && !operators.isEmpty();
        out.emit(charClass.negated() ? "[^" : "[", origin);
        if (wrapOperands) {
            out.emit("[", origin);
        }
        emitOperands(operands, operators);
        if (wrapOperands) {
            out.emit("]", origin);
        }
        out.emit("]", charClass.span().end() - 1);
    }

    private static void splitOperands(List<RegexNode> items, List<List<RegexNode>> operands, List<SetOperator> ops) {
        List<RegexNode> current = new ArrayList<>();
        for (RegexNode item : items) {
            if (item instanceof RegexNode.ClassOperator operator) {
                operands.add(current);
                ops.add(operator.operator());
                current = new ArrayList<>();
            } else {
                current.add(item);
            }
        }
        operands.add(current);
    }

    /** Java: {@code A&&[B]} irisan, {@code A&&[^B]} selisih (JS flag v {@code A--B}). */
    private void emitOperands(List<List<RegexNode>> operands, List<SetOperator> operators) {
        operands.getFirst().forEach(item -> emit(item, new Mode(false, false), true));
        for (int index = 0; index < operators.size(); index++) {
            List<RegexNode> operand = operands.get(index + 1);
            int origin = operand.isEmpty() ? 0 : operand.getFirst().span().start();
            out.emit(operators.get(index) == SetOperator.INTERSECTION ? "&&[" : "&&[^", origin);
            operand.forEach(item -> emit(item, new Mode(false, false), true));
            out.emit("]", origin);
        }
    }

    private static String range(RegexNode.ClassRange range) {
        return literal(range.from()) + "-" + literal(range.to());
    }

    /** Selalu berbentuk grup supaya digit literal sesudahnya tidak ikut dibaca sebagai nomor grup. */
    private String backreference(RegexNode.Backreference reference) {
        int number = reference.name().isEmpty()
                ? reference.number()
                : numbersByName.getOrDefault(reference.name(), reference.number());
        return "(?:\\" + number + ")";
    }

    private void unsupported(Span span, Feature feature) {
        problems.add(RegexProblem.of(span, ProblemCode.UNSUPPORTED_BY_ENGINE, feature.name()));
    }

    /**
     * Literal aman untuk Java, di dalam maupun di luar class: huruf/digit ASCII apa adanya, tanda baca ASCII di-escape
     * (Java mengizinkan escape karakter non-alfabet apa pun), selain itu {@code \\x{..}}.
     */
    static String literal(int codePoint) {
        if (codePoint < ASCII_DELETE && Character.isLetterOrDigit(codePoint)) {
            return String.valueOf((char) codePoint);
        }
        if (codePoint > ASCII_SPACE && codePoint < ASCII_DELETE) {
            return "\\" + (char) codePoint;
        }
        return "\\x{" + Integer.toHexString(codePoint) + "}";
    }

    /** State flag yang berubah lewat grup/flag inline. */
    private static final class Mode {

        private boolean multiline;
        private boolean dotAll;

        private Mode(boolean multiline, boolean dotAll) {
            this.multiline = multiline;
            this.dotAll = dotAll;
        }

        private Mode copy() {
            return new Mode(multiline, dotAll);
        }
    }
}
