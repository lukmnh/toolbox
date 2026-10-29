package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.List;
import java.util.Map;

/** Setelah seluruh pola di-parse: backreference harus menunjuk grup yang ada. */
final class ReferenceValidator {

    private final ParseCursor in;
    private final ParserDialect dialect;
    private final Map<Integer, GroupInfo> groups;
    private final Map<String, Integer> names;

    ReferenceValidator(
            ParseCursor in, ParserDialect dialect, Map<Integer, GroupInfo> groups, Map<String, Integer> names) {
        this.in = in;
        this.dialect = dialect;
        this.groups = groups;
        this.names = names;
    }

    void validate(List<RegexNode.Backreference> references) {
        for (RegexNode.Backreference reference : references) {
            boolean exists = reference.name().isEmpty()
                    ? groups.containsKey(reference.number())
                    : names.containsKey(reference.name());
            // Go sudah ditandai "tidak didukung"; tidak perlu pesan kedua untuk referensi yang sama
            if (!exists && !dialect.is(RegexFlavor.GO)) {
                String target = reference.name().isEmpty() ? Integer.toString(reference.number()) : reference.name();
                in.problem(reference.span(), ProblemCode.UNKNOWN_GROUP_REFERENCE, target);
            }
        }
    }
}
