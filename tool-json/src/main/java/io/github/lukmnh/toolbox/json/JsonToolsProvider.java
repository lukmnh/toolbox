package io.github.lukmnh.toolbox.json;

import io.github.lukmnh.toolbox.common.io.AppDirectories;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolProvider;
import io.github.lukmnh.toolbox.json.beautify.application.FormatJsonUseCase;
import io.github.lukmnh.toolbox.json.beautify.application.LoadJsonFileUseCase;
import io.github.lukmnh.toolbox.json.beautify.application.ReadOutputUseCase;
import io.github.lukmnh.toolbox.json.beautify.domain.JsonFormatter;
import io.github.lukmnh.toolbox.json.beautify.infrastructure.JacksonJsonFormatter;
import io.github.lukmnh.toolbox.json.beautify.presentation.BeautifyTool;
import io.github.lukmnh.toolbox.json.csv.application.ExportCsvUseCase;
import io.github.lukmnh.toolbox.json.csv.application.LoadSampleUseCase;
import io.github.lukmnh.toolbox.json.csv.application.OpenCsvFileUseCase;
import io.github.lukmnh.toolbox.json.csv.application.PreviewPipelineUseCase;
import io.github.lukmnh.toolbox.json.csv.domain.CsvFiles;
import io.github.lukmnh.toolbox.json.csv.domain.TimedMatcher;
import io.github.lukmnh.toolbox.json.csv.infrastructure.FastCsvFiles;
import io.github.lukmnh.toolbox.json.csv.infrastructure.TimedRegexMatcher;
import io.github.lukmnh.toolbox.json.csv.presentation.CsvTool;
import io.github.lukmnh.toolbox.json.csv.presentation.CsvUseCases;
import io.github.lukmnh.toolbox.json.viewer.application.ExportViewUseCase;
import io.github.lukmnh.toolbox.json.viewer.application.FilterRecordsUseCase;
import io.github.lukmnh.toolbox.json.viewer.application.OpenJsonFileUseCase;
import io.github.lukmnh.toolbox.json.viewer.application.RecentJsonFilesUseCase;
import io.github.lukmnh.toolbox.json.viewer.application.SortRecordsUseCase;
import io.github.lukmnh.toolbox.json.viewer.infrastructure.FileViewExporter;
import io.github.lukmnh.toolbox.json.viewer.infrastructure.JacksonJsonFiles;
import io.github.lukmnh.toolbox.json.viewer.infrastructure.JsonRecentFiles;
import io.github.lukmnh.toolbox.json.viewer.presentation.ViewerTool;
import io.github.lukmnh.toolbox.json.viewer.presentation.ViewerUseCases;
import java.util.List;

/**
 * Composition root module JSON: merakit infrastructure → use case → tool secara manual (tanpa DI framework).
 * Didaftarkan di {@code META-INF/services/io.github.lukmnh.toolbox.common.tool.ToolProvider}.
 */
public final class JsonToolsProvider implements ToolProvider {

    @Override
    public List<Tool> tools() {
        return List.of(viewerTool(), beautifyTool(), csvTool());
    }

    private static ViewerTool viewerTool() {
        ViewerUseCases useCases = new ViewerUseCases(
                new OpenJsonFileUseCase(new JacksonJsonFiles()),
                new FilterRecordsUseCase(),
                new SortRecordsUseCase(),
                new ExportViewUseCase(new FileViewExporter()),
                new RecentJsonFilesUseCase(new JsonRecentFiles(AppDirectories.dataDirectory())));
        return new ViewerTool(useCases);
    }

    private static BeautifyTool beautifyTool() {
        JsonFormatter formatter = new JacksonJsonFormatter();
        return new BeautifyTool(
                new LoadJsonFileUseCase(formatter), new FormatJsonUseCase(formatter), new ReadOutputUseCase(formatter));
    }

    private static CsvTool csvTool() {
        CsvFiles files = new FastCsvFiles();
        TimedMatcher matcher = new TimedRegexMatcher(SecurityLimits.REGEX_TIMEOUT);
        return new CsvTool(new CsvUseCases(
                new OpenCsvFileUseCase(files, SecurityLimits.CSV_MAX_FILE_BYTES),
                new LoadSampleUseCase(files),
                new PreviewPipelineUseCase(matcher),
                new ExportCsvUseCase(files, matcher)));
    }
}
