package io.github.lukmnh.toolbox.pdf.split.domain;

import java.nio.file.Path;
import java.util.List;

public record SplitResult(Path outputDirectory, List<Path> files) {

    public SplitResult {
        files = List.copyOf(files);
    }
}
