package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.devutils.regex.domain.Feature;
import io.github.lukmnh.toolbox.devutils.regex.domain.InvalidReplacementException;
import io.github.lukmnh.toolbox.devutils.regex.domain.ProblemCode;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexCompileException;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlag;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlavor;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexIoException;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexProblem;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexTooComplexException;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexToolException;
import io.github.lukmnh.toolbox.devutils.regex.domain.TestTextTooLargeException;
import io.github.lukmnh.toolbox.devutils.regex.domain.UnsupportedRegexException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/** Memetakan flavor, flag, masalah pola, dan error domain Regex Tester ke teks berbahasa Indonesia. */
final class RegexMessages {

    private static final String PREFIX = "devutils.regex.";

    private RegexMessages() {}

    static String flavorName(RegexFlavor flavor) {
        return Messages.get(PREFIX + "flavor." + flavor.name());
    }

    static String flagTooltip(RegexFlag flag) {
        return Messages.get(PREFIX + "flag." + flag.name());
    }

    static String featureName(Feature feature) {
        return Messages.get(PREFIX + "feature." + feature.name());
    }

    static String number(long value) {
        return NumberFormat.getIntegerInstance(Messages.LOCALE).format(value);
    }

    /** Argumen fitur (nama enum) diganti nama yang bisa dibaca, plus nama flavor sebagai argumen tambahan. */
    static String problem(RegexProblem problem, RegexFlavor flavor) {
        List<Object> args = new ArrayList<>(problem.args());
        boolean featureProblem = problem.code() == ProblemCode.UNSUPPORTED_BY_FLAVOR
                || problem.code() == ProblemCode.UNSUPPORTED_BY_ENGINE;
        if (featureProblem && !args.isEmpty()) {
            args.set(0, featureLabel(args.getFirst().toString()));
        }
        args.add(flavorName(flavor));
        return Messages.format(PREFIX + "problem." + problem.code().name(), args.toArray());
    }

    /** Pesan untuk error engine/domain; posisi ditangani terpisah lewat {@link #position}. */
    static String describe(RegexToolException error, RegexFlavor flavor) {
        return switch (error) {
            case RegexCompileException e -> Messages.format(PREFIX + "error.compile", e.description());
            case UnsupportedRegexException e -> unsupported(e, flavor);
            case InvalidReplacementException e -> Messages.format(PREFIX + "error.replacement", e.reference());
            case TestTextTooLargeException e ->
                Messages.format(
                        PREFIX + "error.tooLarge",
                        e.fileName(),
                        ByteSizeFormat.format(e.sizeBytes()),
                        ByteSizeFormat.format(e.maxBytes()));
            case RegexIoException e -> Messages.format(PREFIX + "error.io", e.fileName());
            case RegexTooComplexException e -> Messages.get(PREFIX + "error.tooComplex");
        };
    }

    /** Posisi error di pola asli, atau -1 bila tidak diketahui / tidak relevan. */
    static int position(RegexToolException error) {
        return switch (error) {
            case RegexCompileException e -> e.position();
            case UnsupportedRegexException e -> e.problems().getFirst().span().start();
            case InvalidReplacementException e -> -1;
            case TestTextTooLargeException e -> -1;
            case RegexIoException e -> -1;
            case RegexTooComplexException e -> -1;
        };
    }

    private static String unsupported(UnsupportedRegexException error, RegexFlavor flavor) {
        String first = problem(error.problems().getFirst(), flavor);
        int others = error.problems().size() - 1;
        return others == 0 ? first : Messages.format(PREFIX + "error.moreProblems", first, number(others));
    }

    private static String featureLabel(String featureName) {
        try {
            return featureName(Feature.valueOf(featureName));
        } catch (IllegalArgumentException e) {
            // Argumen bukan nama Feature (misalnya dari versi parser lain): tampilkan apa adanya
            return featureName;
        }
    }
}
