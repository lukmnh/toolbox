package io.github.lukmnh.toolbox.pdf.merge.domain;

import java.nio.file.Path;

public record MergeResult(Path output, int fileCount, int pageCount) {}
