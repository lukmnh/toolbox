package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Menyusun pohon penjelasan dari {@link ParseResult}. Hasilnya terstruktur (jenis + argumen), teks bahasa Indonesia
 * disusun di presentation supaya domain bebas dari teks UI.
 */
public final class RegexExplainer {

    private RegexExplainer() {}

    public static List<Explanation> explain(ParseResult parsed, Set<RegexFlag> flags) {
        RegexNode root = parsed.root();
        if (isEmpty(root)) {
            return List.of(emptyLine(0, root.span()));
        }
        Walker walker = new Walker(ModeState.from(parsed.flavor(), flags));
        walker.node(root, 0, Quantity.ONCE, root.span());
        return List.copyOf(walker.lines);
    }

    private static boolean isEmpty(RegexNode node) {
        return node instanceof RegexNode.Sequence sequence && sequence.items().isEmpty();
    }

    private static Explanation emptyLine(int depth, Span span) {
        return new Explanation(depth, ExplanationKind.EMPTY, List.of(), Quantity.ONCE, span, 0);
    }

    /** Flag yang memengaruhi arti {@code .} dan {@code ^ $}; berubah lewat flag inline dan grup flag. */
    private record ModeState(boolean dotAll, boolean multiline) {

        static ModeState from(RegexFlavor flavor, Set<RegexFlag> flags) {
            boolean dotAll = flags.stream().anyMatch(flag -> flag.flavor() == flavor && isDotAll(flag));
            boolean multiline = flags.stream().anyMatch(flag -> flag.flavor() == flavor && isMultiline(flag));
            return new ModeState(dotAll, multiline);
        }

        private static boolean isDotAll(RegexFlag flag) {
            return switch (flag) {
                case JAVA_DOTALL, JS_DOT_ALL, GO_DOT_NEWLINE, PHP_DOTALL -> true;
                default -> false;
            };
        }

        private static boolean isMultiline(RegexFlag flag) {
            return switch (flag) {
                case JAVA_MULTILINE, JS_MULTILINE, GO_MULTILINE, PHP_MULTILINE -> true;
                default -> false;
            };
        }

        ModeState apply(String flagsOn, String flagsOff) {
            boolean newDotAll = flagsOff.indexOf('s') >= 0 ? false : dotAll || flagsOn.indexOf('s') >= 0;
            boolean newMultiline = flagsOff.indexOf('m') >= 0 ? false : multiline || flagsOn.indexOf('m') >= 0;
            return new ModeState(newDotAll, newMultiline);
        }
    }

    private static final class Walker {

        private final List<Explanation> lines = new ArrayList<>();
        private ModeState mode;

        Walker(ModeState mode) {
            this.mode = mode;
        }

        /** @param span rentang yang di-highlight saat hover; untuk atom ber-quantifier mencakup quantifier-nya */
        void node(RegexNode node, int depth, Quantity quantity, Span span) {
            switch (node) {
                case RegexNode.Alternation alternation -> alternation(alternation, depth, quantity);
                case RegexNode.Sequence sequence -> sequence(sequence, depth);
                case RegexNode.Group group -> group(group, depth, quantity, span);
                case RegexNode.Quantified quantified ->
                    node(
                            quantified.target(),
                            depth,
                            new Quantity(quantified.min(), quantified.max(), quantified.mode()),
                            quantified.span());
                case RegexNode.Literal literal -> literal(literal, depth, quantity, span);
                case RegexNode.CharClass charClass -> charClass(charClass, depth, quantity, span);
                case RegexNode.InlineFlags flags -> inlineFlags(flags, depth);
                default -> leaf(node, depth, quantity, span);
            }
        }

        private void alternation(RegexNode.Alternation alternation, int depth, Quantity quantity) {
            add(depth, ExplanationKind.ALTERNATION, List.of(), quantity, alternation.span(), 0);
            for (int index = 0; index < alternation.branches().size(); index++) {
                RegexNode branch = alternation.branches().get(index);
                add(
                        depth + 1,
                        ExplanationKind.ALTERNATIVE,
                        List.of(String.valueOf(index + 1)),
                        Quantity.ONCE,
                        branch.span(),
                        0);
                body(branch, depth + 2);
            }
        }

        /** Isi grup/cabang: urutan kosong ditampilkan sebagai "kosong" supaya cabang {@code a|} tetap terlihat. */
        private void body(RegexNode node, int depth) {
            if (isEmpty(node)) {
                lines.add(emptyLine(depth, node.span()));
                return;
            }
            node(node, depth, Quantity.ONCE, node.span());
        }

        /** Literal berurutan tanpa quantifier digabung menjadi satu baris teks. */
        private void sequence(RegexNode.Sequence sequence, int depth) {
            StringBuilder text = new StringBuilder();
            int textStart = -1;
            int textEnd = -1;
            for (RegexNode item : sequence.items()) {
                if (item instanceof RegexNode.Literal literal && !isInvisible(literal.codePoint())) {
                    textStart = textStart < 0 ? literal.span().start() : textStart;
                    textEnd = literal.span().end();
                    text.appendCodePoint(literal.codePoint());
                    continue;
                }
                flushText(text, textStart, textEnd, depth);
                textStart = -1;
                node(item, depth, Quantity.ONCE, item.span());
            }
            flushText(text, textStart, textEnd, depth);
        }

        private void flushText(StringBuilder text, int start, int end, int depth) {
            if (text.isEmpty()) {
                return;
            }
            add(depth, ExplanationKind.LITERAL_TEXT, List.of(text.toString()), Quantity.ONCE, new Span(start, end), 0);
            text.setLength(0);
        }

        private void group(RegexNode.Group group, int depth, Quantity quantity, Span span) {
            ModeState outer = mode;
            if (group.kind() == GroupKind.FLAG_GROUP) {
                mode = mode.apply(group.flagsOn(), group.flagsOff());
            }
            int groupNumber =
                    group.kind() == GroupKind.CAPTURING || group.kind() == GroupKind.NAMED ? group.number() : 0;
            add(depth, groupKind(group.kind()), groupArgs(group), quantity, span, groupNumber);
            body(group.body(), depth + 1);
            // Flag inline di dalam grup tidak berlaku setelah grup ditutup
            mode = outer;
        }

        private static ExplanationKind groupKind(GroupKind kind) {
            return switch (kind) {
                case CAPTURING -> ExplanationKind.GROUP_CAPTURING;
                case NAMED -> ExplanationKind.GROUP_NAMED;
                case NON_CAPTURING -> ExplanationKind.GROUP_NON_CAPTURING;
                case ATOMIC -> ExplanationKind.GROUP_ATOMIC;
                case LOOKAHEAD -> ExplanationKind.LOOKAHEAD;
                case NEGATIVE_LOOKAHEAD -> ExplanationKind.NEGATIVE_LOOKAHEAD;
                case LOOKBEHIND -> ExplanationKind.LOOKBEHIND;
                case NEGATIVE_LOOKBEHIND -> ExplanationKind.NEGATIVE_LOOKBEHIND;
                case FLAG_GROUP -> ExplanationKind.GROUP_FLAGS;
            };
        }

        private static List<String> groupArgs(RegexNode.Group group) {
            return switch (group.kind()) {
                case CAPTURING -> List.of(String.valueOf(group.number()));
                case NAMED -> List.of(String.valueOf(group.number()), group.name());
                case FLAG_GROUP -> List.of(group.flagsOn(), group.flagsOff());
                default -> List.of();
            };
        }

        private void literal(RegexNode.Literal literal, int depth, Quantity quantity, Span span) {
            int codePoint = literal.codePoint();
            if (isInvisible(codePoint)) {
                add(depth, ExplanationKind.LITERAL_SPECIAL, List.of(hex(codePoint)), quantity, span, 0);
                return;
            }
            add(depth, ExplanationKind.LITERAL_TEXT, List.of(Character.toString(codePoint)), quantity, span, 0);
        }

        private void charClass(RegexNode.CharClass charClass, int depth, Quantity quantity, Span span) {
            ExplanationKind kind =
                    charClass.negated() ? ExplanationKind.NEGATED_CHAR_CLASS : ExplanationKind.CHAR_CLASS;
            add(depth, kind, List.of(), quantity, span, 0);
            for (RegexNode item : charClass.items()) {
                classItem(item, depth + 1);
            }
        }

        private void classItem(RegexNode item, int depth) {
            switch (item) {
                case RegexNode.Literal literal
                when isInvisible(literal.codePoint()) ->
                    add(
                            depth,
                            ExplanationKind.LITERAL_SPECIAL,
                            List.of(hex(literal.codePoint())),
                            Quantity.ONCE,
                            literal.span(),
                            0);
                case RegexNode.Literal literal ->
                    add(
                            depth,
                            ExplanationKind.CLASS_LITERAL,
                            List.of(Character.toString(literal.codePoint())),
                            Quantity.ONCE,
                            literal.span(),
                            0);
                case RegexNode.ClassRange range ->
                    add(
                            depth,
                            ExplanationKind.CLASS_RANGE,
                            List.of(display(range.from()), display(range.to())),
                            Quantity.ONCE,
                            range.span(),
                            0);
                case RegexNode.ClassOperator operator ->
                    add(
                            depth,
                            ExplanationKind.CLASS_OPERATOR,
                            List.of(operator.operator().name()),
                            Quantity.ONCE,
                            operator.span(),
                            0);
                default -> node(item, depth, Quantity.ONCE, item.span());
            }
        }

        private void inlineFlags(RegexNode.InlineFlags flags, int depth) {
            add(
                    depth,
                    ExplanationKind.INLINE_FLAGS,
                    List.of(flags.flagsOn(), flags.flagsOff()),
                    Quantity.ONCE,
                    flags.span(),
                    0);
            mode = mode.apply(flags.flagsOn(), flags.flagsOff());
        }

        private void leaf(RegexNode node, int depth, Quantity quantity, Span span) {
            Leaf leaf = describe(node);
            add(depth, leaf.kind(), leaf.args(), quantity, span, 0);
        }

        private Leaf describe(RegexNode node) {
            return switch (node) {
                case RegexNode.AnyChar any -> new Leaf(ExplanationKind.ANY_CHAR, List.of(bool(mode.dotAll())));
                case RegexNode.Anchor anchor ->
                    new Leaf(ExplanationKind.ANCHOR, List.of(anchor.kind().name(), bool(mode.multiline())));
                case RegexNode.Shorthand shorthand ->
                    new Leaf(ExplanationKind.SHORTHAND, List.of(shorthand.kind().name()));
                case RegexNode.UnicodeProperty property ->
                    new Leaf(ExplanationKind.UNICODE_PROPERTY, List.of(property.name(), bool(property.negated())));
                case RegexNode.PosixClass posix ->
                    new Leaf(ExplanationKind.POSIX_CLASS, List.of(posix.name(), bool(posix.negated())));
                case RegexNode.Backreference reference ->
                    reference.number() > 0
                            ? new Leaf(
                                    ExplanationKind.BACKREFERENCE_NUMBER, List.of(String.valueOf(reference.number())))
                            : new Leaf(ExplanationKind.BACKREFERENCE_NAME, List.of(reference.name()));
                case RegexNode.QuotedLiteral quoted -> new Leaf(ExplanationKind.QUOTED, List.of(quoted.text()));
                case RegexNode.Comment comment -> new Leaf(ExplanationKind.COMMENT, List.of(comment.text()));
                case RegexNode.SpecialConstruct special ->
                    new Leaf(ExplanationKind.SPECIAL, List.of(special.kind().name(), special.raw()));
                case RegexNode.Unknown unknown -> new Leaf(ExplanationKind.UNKNOWN, List.of(unknown.raw()));
                case RegexNode.ClassRange range ->
                    new Leaf(ExplanationKind.CLASS_RANGE, List.of(display(range.from()), display(range.to())));
                case RegexNode.ClassOperator operator ->
                    new Leaf(
                            ExplanationKind.CLASS_OPERATOR,
                            List.of(operator.operator().name()));
                // Node komposit (grup, urutan, class, literal, quantifier) ditangani langsung di node()
                default ->
                    throw new IllegalStateException(
                            "Bukan node daun: " + node.getClass().getSimpleName());
            };
        }

        private void add(
                int depth, ExplanationKind kind, List<String> args, Quantity quantity, Span span, int groupNumber) {
            lines.add(new Explanation(depth, kind, args, quantity, span, groupNumber));
        }
    }

    private record Leaf(ExplanationKind kind, List<String> args) {}

    /** Karakter yang tidak terlihat jika ditampilkan apa adanya: spasi, tab, newline, kontrol, format. */
    static boolean isInvisible(int codePoint) {
        if (Character.isWhitespace(codePoint)
                || Character.isSpaceChar(codePoint)
                || Character.isISOControl(codePoint)) {
            return true;
        }
        int type = Character.getType(codePoint);
        return type == Character.FORMAT
                || type == Character.UNASSIGNED
                || type == Character.PRIVATE_USE
                || type == Character.SURROGATE;
    }

    /** Kode Unicode heksadesimal minimal 4 digit, huruf besar (misalnya {@code 0009}). */
    static String hex(int codePoint) {
        return String.format(Locale.ROOT, "%04X", codePoint);
    }

    /** Ujung range: karakter apa adanya bila terlihat, selain itu {@code U+XXXX}. */
    private static String display(int codePoint) {
        return isInvisible(codePoint) ? "U+" + hex(codePoint) : Character.toString(codePoint);
    }

    private static String bool(boolean value) {
        return Boolean.toString(value);
    }
}
