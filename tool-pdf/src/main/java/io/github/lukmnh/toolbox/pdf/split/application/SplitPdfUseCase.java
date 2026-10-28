package io.github.lukmnh.toolbox.pdf.split.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.pdf.shared.domain.PageRange;
import io.github.lukmnh.toolbox.pdf.split.domain.PdfSplitter;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitMode;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitRequest;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitResult;
import io.github.lukmnh.toolbox.pdf.split.domain.SplitSession;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Memisah PDF per halaman atau per range; tiap range menjadi satu file. */
public final class SplitPdfUseCase {

    private static final String PDF_EXTENSION = ".pdf";

    private final PdfSplitter splitter;

    public SplitPdfUseCase(PdfSplitter splitter) {
        this.splitter = splitter;
    }

    /**
     * @throws io.github.lukmnh.toolbox.pdf.shared.domain.InvalidPageRangeException bila range tidak valid
     */
    public SplitResult execute(SplitRequest request, ProgressReporter progress, CancellationToken cancellation) {
        try (SplitSession session = splitter.open(request.source())) {
            List<PageRange> ranges = rangesFor(request.mode(), session.pageCount());
            List<Path> files = new ArrayList<>(ranges.size());
            Set<String> usedNames = new HashSet<>();
            for (PageRange range : ranges) {
                cancellation.throwIfCancelled();
                Path output = request.outputDirectory().resolve(uniqueName(request.baseName(), range, usedNames));
                session.extract(range, output, request.protection());
                files.add(output);
                progress.report(files.size(), ranges.size());
            }
            return new SplitResult(request.outputDirectory(), files);
        }
    }

    static List<PageRange> rangesFor(SplitMode mode, int pageCount) {
        return switch (mode) {
            case SplitMode.EachPage eachPage -> PageRange.eachPage(pageCount);
            case SplitMode.Ranges ranges -> PageRange.parseList(ranges.specification(), pageCount);
        };
    }

    /** Range yang sama ditulis dua kali (misalnya "1, 1") tidak boleh saling menimpa. */
    private static String uniqueName(String baseName, PageRange range, Set<String> usedNames) {
        String stem = baseName + "_" + range.label();
        String name = stem + PDF_EXTENSION;
        for (int counter = 2; !usedNames.add(name); counter++) {
            name = stem + "_" + counter + PDF_EXTENSION;
        }
        return name;
    }
}
