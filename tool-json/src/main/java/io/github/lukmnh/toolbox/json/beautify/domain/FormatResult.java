package io.github.lukmnh.toolbox.json.beautify.domain;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/** @param output file hasil; {@code null} untuk Validate */
public record FormatResult(
        BeautifyAction action, Path output, long inputBytes, long outputBytes, FormatStats stats, Duration duration) {

    public Optional<Path> outputFile() {
        return Optional.ofNullable(output);
    }
}
