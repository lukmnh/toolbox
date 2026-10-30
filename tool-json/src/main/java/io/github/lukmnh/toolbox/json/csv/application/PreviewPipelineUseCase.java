package io.github.lukmnh.toolbox.json.csv.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.json.csv.domain.ColumnPipeline;
import io.github.lukmnh.toolbox.json.csv.domain.CsvRow;
import io.github.lukmnh.toolbox.json.csv.domain.CsvSample;
import io.github.lukmnh.toolbox.json.csv.domain.MismatchPolicy;
import io.github.lukmnh.toolbox.json.csv.domain.PipelinePreview;
import io.github.lukmnh.toolbox.json.csv.domain.PipelineStep;
import io.github.lukmnh.toolbox.json.csv.domain.RowNormalizer;
import io.github.lukmnh.toolbox.json.csv.domain.TablePreview;
import io.github.lukmnh.toolbox.json.csv.domain.TimedMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Jalankan pipeline atas sampel untuk pratinjau hasil; logikanya sama persis dengan ekspor. */
public final class PreviewPipelineUseCase {

    private final TimedMatcher matcher;

    public PreviewPipelineUseCase(TimedMatcher matcher) {
        this.matcher = matcher;
    }

    /**
     * @throws io.github.lukmnh.toolbox.json.csv.domain.InvalidStepException bila ada langkah tidak valid
     * @throws io.github.lukmnh.toolbox.json.csv.domain.RegexSplitException bila regex split melewati batas waktu
     */
    public PipelinePreview execute(
            CsvSample sample, MismatchPolicy policy, List<PipelineStep> steps, CancellationToken cancellation) {
        ColumnPipeline pipeline = ColumnPipeline.compile(sample.columns(), steps);
        RowNormalizer normalizer = new RowNormalizer(sample.columns().size(), policy);
        List<List<String>> rows = new ArrayList<>(sample.rows().size());
        for (CsvRow row : sample.rows()) {
            cancellation.throwIfCancelled();
            Optional<List<String>> normalized = normalizer.normalize(row);
            normalized.ifPresent(fields -> rows.add(pipeline.apply(fields, matcher)));
        }
        return new PipelinePreview(new TablePreview(pipeline.outputColumns(), rows), normalizer.report());
    }
}
