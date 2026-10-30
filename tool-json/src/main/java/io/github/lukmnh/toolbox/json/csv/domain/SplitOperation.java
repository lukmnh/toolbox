package io.github.lukmnh.toolbox.json.csv.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Split yang sudah di-resolve: jumlah kolom hasil tetap, apa pun isi nilainya. */
final class SplitOperation implements ColumnOperation {

    private static final String NAME_SEPARATOR = "_";
    private static final String POSITION_SEPARATOR = ",";

    private final int sourceIndex;
    private final boolean keepSource;
    private final int partCount;
    private final Splitter splitter;
    private final List<String> outputColumns;

    private SplitOperation(
            int sourceIndex, boolean keepSource, int partCount, Splitter splitter, List<String> outputColumns) {
        this.sourceIndex = sourceIndex;
        this.keepSource = keepSource;
        this.partCount = partCount;
        this.splitter = splitter;
        this.outputColumns = outputColumns;
    }

    static SplitOperation compile(int stepNumber, List<String> columns, SplitStep step) {
        int sourceIndex = ColumnPipeline.indexOf(stepNumber, columns, step.sourceColumn());
        Splitter splitter = splitter(stepNumber, step.method());
        int partCount = Math.min(step.maxColumns(), splitter.naturalPartCount(step.maxColumns()));
        List<String> names = resultNames(step, partCount);
        List<String> output = new ArrayList<>(columns.size() + partCount);
        for (int index = 0; index < columns.size(); index++) {
            if (index != sourceIndex || step.keepSource()) {
                output.add(columns.get(index));
            }
            if (index == sourceIndex) {
                output.addAll(names);
            }
        }
        return new SplitOperation(sourceIndex, step.keepSource(), partCount, splitter, List.copyOf(output));
    }

    @Override
    public List<String> outputColumns() {
        return outputColumns;
    }

    @Override
    public List<String> apply(List<String> row, TimedMatcher matcher) {
        List<String> output = new ArrayList<>(outputColumns.size());
        for (int index = 0; index < row.size(); index++) {
            if (index != sourceIndex || keepSource) {
                output.add(row.get(index));
            }
            if (index == sourceIndex) {
                output.addAll(fit(splitter.split(row.get(index), partCount, matcher)));
            }
        }
        return output;
    }

    /** Tepat {@code partCount} bagian: kurang (tidak match/nilai pendek) diisi kosong, group berlebih dibuang. */
    private List<String> fit(List<String> parts) {
        if (parts.size() == partCount) {
            return parts;
        }
        List<String> fitted = new ArrayList<>(parts.subList(0, Math.min(parts.size(), partCount)));
        while (fitted.size() < partCount) {
            fitted.add("");
        }
        return fitted;
    }

    private static List<String> resultNames(SplitStep step, int partCount) {
        List<String> names = new ArrayList<>(partCount);
        for (int index = 0; index < partCount; index++) {
            String given = index < step.resultNames().size()
                    ? step.resultNames().get(index).strip()
                    : "";
            names.add(given.isEmpty() ? step.sourceColumn() + NAME_SEPARATOR + (index + 1) : given);
        }
        return names;
    }

    private static Splitter splitter(int stepNumber, SplitMethod method) {
        return switch (method) {
            case SplitMethod.BySeparator separator -> separatorSplitter(stepNumber, separator.separator());
            case SplitMethod.ByPositions positions -> new PositionSplitter(parsePositions(stepNumber, positions));
            case SplitMethod.ByRegex regex -> regexSplitter(stepNumber, regex.pattern());
        };
    }

    private static Splitter separatorSplitter(int stepNumber, String separator) {
        if (separator.isEmpty()) {
            throw new InvalidStepException(stepNumber, StepProblem.EMPTY_SEPARATOR, "");
        }
        // LITERAL: pemisah dari user bukan regex, jadi tidak ada risiko backtracking
        return new SeparatorSplitter(Pattern.compile(separator, Pattern.LITERAL));
    }

    private static int[] parsePositions(int stepNumber, SplitMethod.ByPositions method) {
        String text = method.positions().strip();
        if (text.isEmpty()) {
            throw new InvalidStepException(stepNumber, StepProblem.INVALID_POSITIONS, "");
        }
        try {
            int[] cuts = Arrays.stream(text.split(POSITION_SEPARATOR))
                    .map(String::strip)
                    .mapToInt(Integer::parseInt)
                    .toArray();
            requireAscending(stepNumber, cuts, text);
            return cuts;
        } catch (NumberFormatException e) {
            throw new InvalidStepException(stepNumber, StepProblem.INVALID_POSITIONS, text);
        }
    }

    private static void requireAscending(int stepNumber, int[] cuts, String text) {
        int previous = 0;
        for (int cut : cuts) {
            if (cut <= previous) {
                throw new InvalidStepException(stepNumber, StepProblem.INVALID_POSITIONS, text);
            }
            previous = cut;
        }
    }

    private static Splitter regexSplitter(int stepNumber, String pattern) {
        try {
            Pattern compiled = Pattern.compile(pattern);
            int groups = compiled.matcher("").groupCount();
            if (groups == 0) {
                throw new InvalidStepException(stepNumber, StepProblem.REGEX_WITHOUT_GROUP, pattern);
            }
            return new RegexSplitter(compiled, groups);
        } catch (PatternSyntaxException e) {
            throw new InvalidStepException(stepNumber, StepProblem.INVALID_REGEX, e.getDescription());
        }
    }

    /** Strategi pemecahan satu nilai. */
    private sealed interface Splitter {

        /** Jumlah bagian yang dihasilkan cara ini, sebelum dibatasi maks kolom. */
        int naturalPartCount(int maxColumns);

        List<String> split(String value, int partCount, TimedMatcher matcher);
    }

    private record SeparatorSplitter(Pattern separator) implements Splitter {

        @Override
        public int naturalPartCount(int maxColumns) {
            return maxColumns;
        }

        @Override
        public List<String> split(String value, int partCount, TimedMatcher matcher) {
            // limit = partCount: sisa teks setelah pemisah ke-(n-1) masuk ke kolom terakhir, tidak hilang
            return Arrays.asList(separator.split(value, partCount));
        }
    }

    private record PositionSplitter(int[] cuts) implements Splitter {

        @Override
        public int naturalPartCount(int maxColumns) {
            return cuts.length + 1;
        }

        @Override
        public List<String> split(String value, int partCount, TimedMatcher matcher) {
            int codePoints = value.codePointCount(0, value.length());
            List<String> parts = new ArrayList<>(partCount);
            int start = 0;
            for (int index = 0; index < partCount - 1; index++) {
                int end = Math.min(cuts[index], codePoints);
                parts.add(substring(value, start, end));
                start = end;
            }
            parts.add(substring(value, start, codePoints));
            return parts;
        }

        /** Posisi dihitung per code point supaya emoji/aksara non-Latin tidak terbelah. */
        private static String substring(String value, int startCodePoint, int endCodePoint) {
            if (startCodePoint >= endCodePoint) {
                return "";
            }
            int begin = value.offsetByCodePoints(0, startCodePoint);
            int end = value.offsetByCodePoints(begin, endCodePoint - startCodePoint);
            return value.substring(begin, end);
        }
    }

    private record RegexSplitter(Pattern pattern, int groupCount) implements Splitter {

        @Override
        public int naturalPartCount(int maxColumns) {
            return groupCount;
        }

        @Override
        public List<String> split(String value, int partCount, TimedMatcher matcher) {
            return matcher.captureGroups(pattern, value);
        }
    }
}
