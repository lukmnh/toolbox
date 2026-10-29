package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Template replacement dengan sintaks persis seperti di bahasa aslinya:
 *
 * <ul>
 *   <li>Java ({@code Matcher.appendReplacement}): {@code $n}, {@code ${name}}, {@code \x}; referensi tidak valid → error
 *   <li>JavaScript ({@code String.prototype.replace}): {@code $$ $& $` $' $n $nn $<name>}
 *   <li>Go ({@code Regexp.Expand}): {@code $$ $name ${name}}, nama = deret huruf/angka/underscore terpanjang
 *   <li>PHP ({@code preg_replace}): {@code \n $n ${n}} maks dua digit, tanpa referensi nama
 * </ul>
 */
public final class ReplacementTemplate {

    private final List<Part> parts;

    private ReplacementTemplate(List<Part> parts) {
        this.parts = List.copyOf(parts);
    }

    /**
     * @param groupNames nama grup per nomor (indeks 0 = seluruh match), ukuran = jumlah grup + 1
     * @throws InvalidReplacementException hanya untuk flavor Java, yang menolak referensi tidak valid
     */
    public static ReplacementTemplate parse(String template, RegexFlavor flavor, List<String> groupNames) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(groupNames, "groupNames");
        List<Part> parts = switch (flavor) {
            case JAVA -> new JavaReplacementParser(template, groupNames).parse();
            case JAVASCRIPT -> new JavaScriptReplacementParser(template, groupNames).parse();
            case GO -> new GoReplacementParser(template, groupNames).parse();
            case PHP -> new PhpReplacementParser(template).parse();
        };
        return new ReplacementTemplate(mergeText(parts));
    }

    /** @param matches match terurut dan tidak tumpang tindih */
    public String replaceAll(String input, List<RegexMatch> matches) {
        StringBuilder output = new StringBuilder(input.length());
        int last = 0;
        for (RegexMatch match : matches) {
            output.append(input, last, match.start());
            expand(output, input, match);
            last = match.end();
        }
        return output.append(input, last, input.length()).toString();
    }

    List<Part> parts() {
        return parts;
    }

    private void expand(StringBuilder output, String input, RegexMatch match) {
        for (Part part : parts) {
            switch (part) {
                case Part.Text text -> output.append(text.value());
                case Part.WholeMatch whole -> output.append(match.value());
                case Part.Before before -> output.append(input, 0, match.start());
                case Part.After after -> output.append(input, match.end(), input.length());
                case Part.Group group -> output.append(groupValue(match, group.number()));
            }
        }
    }

    /** Grup yang tidak ada atau tidak ikut cocok diganti string kosong (perilaku semua flavor). */
    private static String groupValue(RegexMatch match, int number) {
        if (number == 0) {
            return match.value();
        }
        if (number > match.groups().size()) {
            return "";
        }
        GroupMatch group = match.groups().get(number - 1);
        return group.matched() ? group.value() : "";
    }

    private static List<Part> mergeText(List<Part> parts) {
        List<Part> merged = new ArrayList<>();
        for (Part part : parts) {
            if (part instanceof Part.Text text && !merged.isEmpty() && merged.getLast() instanceof Part.Text previous) {
                merged.set(merged.size() - 1, new Part.Text(previous.value() + text.value()));
            } else {
                merged.add(part);
            }
        }
        return merged;
    }

    /** Potongan template yang sudah di-resolve. */
    sealed interface Part {

        record Text(String value) implements Part {}

        record WholeMatch() implements Part {}

        record Before() implements Part {}

        record After() implements Part {}

        /** Nomor grup; 0 = seluruh match, nomor di luar jumlah grup → string kosong. */
        record Group(int number) implements Part {}
    }
}
