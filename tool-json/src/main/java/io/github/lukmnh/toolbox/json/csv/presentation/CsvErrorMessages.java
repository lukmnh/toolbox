package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.json.csv.domain.CsvEncodingException;
import io.github.lukmnh.toolbox.json.csv.domain.CsvFileTooLargeException;
import io.github.lukmnh.toolbox.json.csv.domain.CsvIoException;
import io.github.lukmnh.toolbox.json.csv.domain.CsvToolException;
import io.github.lukmnh.toolbox.json.csv.domain.InvalidStepException;
import io.github.lukmnh.toolbox.json.csv.domain.MalformedCsvException;
import io.github.lukmnh.toolbox.json.csv.domain.NotCsvFileException;
import io.github.lukmnh.toolbox.json.csv.domain.RegexSplitException;
import javafx.stage.Window;

/** Memetakan error domain CSV ke pesan berbahasa Indonesia yang menyebut nama file atau nomor langkah. */
final class CsvErrorMessages {

    private static final double MILLIS_PER_SECOND = 1000.0;

    private CsvErrorMessages() {}

    static void show(Window owner, Throwable error) {
        if (error instanceof CsvToolException csvError) {
            ErrorDialogs.show(owner, Messages.get("json.csv.error.header"), describe(csvError));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }

    static String describe(CsvToolException error) {
        return switch (error) {
            case CsvIoException e ->
                Messages.format(
                        e.operation() == CsvIoException.Operation.READ ? "json.csv.error.read" : "json.csv.error.write",
                        e.fileName(),
                        ErrorDialogs.describe(e));
            case CsvFileTooLargeException e ->
                Messages.format(
                        "json.csv.error.tooLarge",
                        e.fileName(),
                        ByteSizeFormat.format(e.sizeBytes()),
                        ByteSizeFormat.format(e.maxBytes()));
            case NotCsvFileException e -> Messages.format("json.csv.error.notCsv", e.fileName());
            case CsvEncodingException e ->
                Messages.format("json.csv.error.encoding", e.fileName(), CsvLabels.encoding(e.encoding()));
            case MalformedCsvException e -> malformed(e);
            case InvalidStepException e -> Messages.format("json.csv.error.step", e.stepNumber(), problem(e));
            case RegexSplitException e -> regex(e);
        };
    }

    private static String malformed(MalformedCsvException error) {
        return switch (error.kind()) {
            case EMPTY -> Messages.format("json.csv.error.empty", error.fileName());
            case UNPARSEABLE ->
                Messages.format("json.csv.error.malformed", error.fileName(), ErrorDialogs.describe(error));
        };
    }

    private static String regex(RegexSplitException error) {
        return switch (error.reason()) {
            case TIMEOUT ->
                Messages.format("json.csv.error.regexTimeout", error.timeout().toMillis() / MILLIS_PER_SECOND);
            case TOO_COMPLEX -> Messages.get("json.csv.error.regexTooComplex");
        };
    }

    private static String problem(InvalidStepException error) {
        String key = switch (error.problem()) {
            case NO_SOURCE_COLUMN -> "json.csv.problem.noSource";
            case MISSING_COLUMN -> "json.csv.problem.missingColumn";
            case EMPTY_SEPARATOR -> "json.csv.problem.emptySeparator";
            case INVALID_POSITIONS -> "json.csv.problem.invalidPositions";
            case INVALID_REGEX -> "json.csv.problem.invalidRegex";
            case REGEX_WITHOUT_GROUP -> "json.csv.problem.regexWithoutGroup";
            case TOO_FEW_MERGE_COLUMNS -> "json.csv.problem.tooFewMergeColumns";
            case DUPLICATE_MERGE_COLUMN -> "json.csv.problem.duplicateMergeColumn";
            case EMPTY_NEW_NAME -> "json.csv.problem.emptyNewName";
            case NO_COLUMNS_LEFT -> "json.csv.problem.noColumnsLeft";
            case DUPLICATE_COLUMN -> "json.csv.problem.duplicateColumn";
        };
        return Messages.format(key, error.detail());
    }
}
