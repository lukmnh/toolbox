package io.github.lukmnh.toolbox.json.beautify.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.json.beautify.domain.BeautifyLimits;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatOptions;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatResult;
import io.github.lukmnh.toolbox.json.beautify.domain.FormatStats;
import io.github.lukmnh.toolbox.json.beautify.domain.JsonFormatter;
import io.github.lukmnh.toolbox.json.beautify.domain.JsonInput;
import java.nio.file.Path;
import java.time.Duration;

/** Beautify, Minify, atau Validate satu input JSON secara streaming. */
public final class FormatJsonUseCase {

    private final JsonFormatter formatter;

    public FormatJsonUseCase(JsonFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * @param output file tujuan untuk Beautify/Minify; diabaikan untuk Validate
     * @throws IllegalArgumentException bila sort keys diminta untuk input di atas batas
     */
    public FormatResult execute(
            JsonInput input,
            FormatOptions options,
            Path output,
            ProgressReporter progress,
            CancellationToken cancellation) {
        long inputBytes = input.sizeBytes();
        if (options.sortKeys() && !BeautifyLimits.canSortKeys(inputBytes)) {
            throw new IllegalArgumentException("Sort keys is only available for input ≤ 50 MB");
        }
        Path target = options.action().writesOutput() ? output : null;
        long started = System.nanoTime();
        FormatStats stats = formatter.format(input, options, target, (done, total) -> {
            cancellation.throwIfCancelled();
            progress.report(done, total);
        });
        Duration duration = Duration.ofNanos(System.nanoTime() - started);
        long outputBytes = target == null ? 0 : formatter.fileSize(target);
        return new FormatResult(options.action(), target, inputBytes, outputBytes, stats, duration);
    }
}
