package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.json.shared.presentation.JsonToolIds;
import java.nio.file.Path;
import java.util.Set;
import javafx.scene.Node;

/** CSV Kolom: Split & Merge. Pipeline langkah kolom atas file CSV besar secara streaming. */
public final class CsvTool implements Tool {

    /** {@code .txt} sengaja tidak diklaim untuk "Open with" karena kebanyakan bukan CSV. */
    private static final Set<String> OPENABLE_EXTENSIONS = Set.of("csv", "tsv");

    private final CsvUseCases useCases;
    private CsvViewModel viewModel;

    public CsvTool(CsvUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return JsonToolIds.CSV;
    }

    @Override
    public String name() {
        return Messages.get("json.csv.name");
    }

    @Override
    public String description() {
        return Messages.get("json.csv.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.JSON;
    }

    @Override
    public Set<String> openableExtensions() {
        return OPENABLE_EXTENSIONS;
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new CsvViewModel(useCases, context, new TempFiles());
        return CsvView.create(viewModel, context);
    }

    @Override
    public void openFile(Path file) {
        if (viewModel != null) {
            viewModel.openFile(file);
        }
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
