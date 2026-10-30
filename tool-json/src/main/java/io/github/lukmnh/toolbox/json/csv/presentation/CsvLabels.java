package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.json.csv.domain.ArrangeStep;
import io.github.lukmnh.toolbox.json.csv.domain.CsvEncoding;
import io.github.lukmnh.toolbox.json.csv.domain.Delimiter;
import io.github.lukmnh.toolbox.json.csv.domain.LineEnding;
import io.github.lukmnh.toolbox.json.csv.domain.MergeStep;
import io.github.lukmnh.toolbox.json.csv.domain.MismatchPolicy;
import io.github.lukmnh.toolbox.json.csv.domain.OutputDelimiter;
import io.github.lukmnh.toolbox.json.csv.domain.PipelineStep;
import io.github.lukmnh.toolbox.json.csv.domain.QuoteMode;
import io.github.lukmnh.toolbox.json.csv.domain.SplitStep;

/** Label UI untuk enum dan langkah pipeline. */
final class CsvLabels {

    private static final String LIST_SEPARATOR = ", ";

    private CsvLabels() {}

    static String delimiter(Delimiter delimiter) {
        return switch (delimiter) {
            case COMMA -> Messages.get("json.csv.delimiter.comma");
            case SEMICOLON -> Messages.get("json.csv.delimiter.semicolon");
            case TAB -> Messages.get("json.csv.delimiter.tab");
            case PIPE -> Messages.get("json.csv.delimiter.pipe");
        };
    }

    static String outputDelimiter(OutputDelimiter delimiter) {
        // Pilihan selain "sama" selalu me-resolve ke pemisah tetap, apa pun inputnya
        return delimiter == OutputDelimiter.SAME_AS_INPUT
                ? Messages.get("json.csv.output.delimiter.same")
                : delimiter(delimiter.resolve(Delimiter.COMMA));
    }

    static String encoding(CsvEncoding encoding) {
        return switch (encoding) {
            case UTF_8 -> Messages.get("json.csv.encoding.utf8");
            case UTF_8_BOM -> Messages.get("json.csv.encoding.utf8Bom");
            case WINDOWS_1252 -> Messages.get("json.csv.encoding.windows1252");
        };
    }

    static String quote(QuoteMode quote) {
        return switch (quote) {
            case MINIMAL -> Messages.get("json.csv.quote.minimal");
            case ALL -> Messages.get("json.csv.quote.all");
        };
    }

    static String lineEnding(LineEnding lineEnding) {
        return switch (lineEnding) {
            case CRLF -> Messages.get("json.csv.lineEnding.crlf");
            case LF -> Messages.get("json.csv.lineEnding.lf");
        };
    }

    static String policy(MismatchPolicy policy) {
        return switch (policy) {
            case PAD -> Messages.get("json.csv.mismatch.pad");
            case SKIP -> Messages.get("json.csv.mismatch.skip");
        };
    }

    static String splitMode(SplitMode mode) {
        return switch (mode) {
            case SEPARATOR -> Messages.get("json.csv.split.mode.separator");
            case POSITIONS -> Messages.get("json.csv.split.mode.positions");
            case REGEX -> Messages.get("json.csv.split.mode.regex");
        };
    }

    static String splitRuleHint(SplitMode mode) {
        return switch (mode) {
            case SEPARATOR -> Messages.get("json.csv.split.rule.separator.hint");
            case POSITIONS -> Messages.get("json.csv.split.rule.positions.hint");
            case REGEX -> Messages.get("json.csv.split.rule.regex.hint");
        };
    }

    static String stepTitle(PipelineStep step) {
        return switch (step) {
            case SplitStep ignored -> Messages.get("json.csv.step.split");
            case MergeStep ignored -> Messages.get("json.csv.step.merge");
            case ArrangeStep ignored -> Messages.get("json.csv.step.arrange");
        };
    }

    /** Satu baris ringkas di daftar langkah. */
    static String stepSummary(int number, PipelineStep step) {
        return switch (step) {
            case SplitStep split ->
                Messages.format("json.csv.step.split.summary", number, split.sourceColumn(), split.maxColumns());
            case MergeStep merge ->
                Messages.format(
                        "json.csv.step.merge.summary",
                        number,
                        String.join(LIST_SEPARATOR, merge.sourceColumns()),
                        merge.newName());
            case ArrangeStep arrange ->
                Messages.format(
                        "json.csv.step.arrange.summary",
                        number,
                        arrange.rules().stream().filter(rule -> !rule.keep()).count());
        };
    }
}
