package io.github.lukmnh.toolbox.devutils.regex.domain;

/** Cek apakah sebuah subpola bisa cocok dengan panjang tak terbatas (Java menolaknya di dalam lookbehind). */
final class UnboundedCheck {

    private UnboundedCheck() {}

    static boolean containsUnbounded(RegexNode node) {
        return switch (node) {
            case RegexNode.Quantified quantified ->
                quantified.max() == RegexNode.Quantified.UNBOUNDED || containsUnbounded(quantified.target());
            case RegexNode.Alternation alternation ->
                alternation.branches().stream().anyMatch(UnboundedCheck::containsUnbounded);
            case RegexNode.Sequence sequence -> sequence.items().stream().anyMatch(UnboundedCheck::containsUnbounded);
            case RegexNode.Group group -> containsUnbounded(group.body());
            // Panjang backreference tidak diketahui saat kompilasi
            case RegexNode.Backreference ignored -> true;
            default -> false;
        };
    }
}
