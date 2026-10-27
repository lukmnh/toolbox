package io.github.lukmnh.toolbox.json.beautify.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.json.beautify.domain.BeautifyAction;
import io.github.lukmnh.toolbox.json.beautify.domain.BeautifyLimits;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatOptions;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatResult;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatStats;
import io.github.lukmnh.toolbox.json.beautify.domain.Indent;
import io.github.lukmnh.toolbox.json.beautify.domain.JsonFormatter;
import io.github.lukmnh.toolbox.json.beautify.domain.JsonInput;
import io.github.lukmnh.toolbox.json.beautify.domain.TextPreview;
import io.github.lukmnh.toolbox.json.shared.domain.NotJsonFileException;
import io.github.lukmnh.toolbox.json.shared.domain.ScanProgress;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FormatJsonUseCaseTest {

    private static final Path OUTPUT = Path.of("out.json");
    private static final JsonInput INPUT = new JsonInput.Text("{\"a\":1}");

    private final FakeFormatter formatter = new FakeFormatter();
    private final FormatJsonUseCase useCase = new FormatJsonUseCase(formatter);

    @Test
    void beautifyWritesOutputAndReportsSizes() {
        FormatResult result = useCase.execute(
                INPUT,
                options(BeautifyAction.BEAUTIFY, false),
                OUTPUT,
                ProgressReporter.NONE,
                CancellationToken.none());

        assertEquals(OUTPUT, formatter.writtenTo);
        assertEquals(OUTPUT, result.outputFile().orElseThrow());
        assertEquals(7, result.inputBytes());
        assertEquals(FakeFormatter.OUTPUT_SIZE, result.outputBytes());
        assertEquals(new FormatStats(1, 0), result.stats());
    }

    @Test
    void validateNeverWritesAFile() {
        FormatResult result = useCase.execute(
                INPUT,
                options(BeautifyAction.VALIDATE, false),
                OUTPUT,
                ProgressReporter.NONE,
                CancellationToken.none());

        assertTrue(formatter.called);
        assertNull(formatter.writtenTo);
        assertTrue(result.outputFile().isEmpty());
        assertEquals(0, result.outputBytes());
    }

    @Test
    void sortKeysAboveLimitIsRejectedBeforeFormatting() {
        JsonInput big = new JsonInput.FileInput(Path.of("big.json"), BeautifyLimits.SORT_KEYS_MAX_BYTES + 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        big,
                        options(BeautifyAction.BEAUTIFY, true),
                        OUTPUT,
                        ProgressReporter.NONE,
                        CancellationToken.none()));
        assertFalse(formatter.called);
    }

    @Test
    void cancellationIsCheckedOnProgress() {
        CancellationToken token = new CancellationToken();
        token.cancel();

        assertThrows(
                TaskCancelledException.class,
                () -> useCase.execute(
                        INPUT, options(BeautifyAction.MINIFY, false), OUTPUT, ProgressReporter.NONE, token));
    }

    @Test
    void loadRejectsNonJsonWithoutTouchingFileSystem() {
        LoadJsonFileUseCase load = new LoadJsonFileUseCase(formatter);

        assertThrows(NotJsonFileException.class, () -> load.execute(Path.of("log.jsonl")));
        assertFalse(formatter.sizeAsked);
        assertEquals(FakeFormatter.OUTPUT_SIZE, load.execute(Path.of("ok.JSON")).sizeBytes());
    }

    @Test
    void copyRefusesOutputAboveTenMegabytes() {
        ReadOutputUseCase read = new ReadOutputUseCase(formatter);
        FormatResult big = new FormatResult(
                BeautifyAction.BEAUTIFY,
                OUTPUT,
                1,
                BeautifyLimits.COPY_MAX_BYTES + 1,
                new FormatStats(0, 0),
                java.time.Duration.ZERO);

        assertThrows(IllegalStateException.class, () -> read.fullText(big));
    }

    private static FormatOptions options(BeautifyAction action, boolean sortKeys) {
        return new FormatOptions(action, Indent.TWO_SPACES, sortKeys);
    }

    private static final class FakeFormatter implements JsonFormatter {
        static final long OUTPUT_SIZE = 42;
        boolean called;
        boolean sizeAsked;
        Path writtenTo;

        @Override
        public long fileSize(Path file) {
            sizeAsked = true;
            return OUTPUT_SIZE;
        }

        @Override
        public FormatStats format(JsonInput input, FormatOptions options, Path output, ScanProgress progress) {
            called = true;
            writtenTo = output;
            progress.update(1, 2);
            return new FormatStats(1, 0);
        }

        @Override
        public TextPreview readText(Path file, int maxBytes) {
            return new TextPreview("", false);
        }
    }
}
