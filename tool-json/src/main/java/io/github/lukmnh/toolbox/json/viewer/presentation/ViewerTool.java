package io.github.lukmnh.toolbox.json.viewer.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import java.nio.file.Path;
import java.util.Set;
import javafx.scene.Node;

/** JSON Viewer; juga tujuan "Open with" untuk {@code .json}, {@code .jsonl}, dan {@code .ndjson}. */
public final class ViewerTool implements Tool {

    private final ViewerUseCases useCases;
    private ViewerViewModel viewModel;

    public ViewerTool(ViewerUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return "json.viewer";
    }

    @Override
    public String name() {
        return Messages.get("json.viewer.name");
    }

    @Override
    public String description() {
        return Messages.get("json.viewer.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.JSON;
    }

    @Override
    public Set<String> openableExtensions() {
        return ViewerViewModel.EXTENSIONS;
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new ViewerViewModel(useCases, context, new TempFiles());
        return new ViewerView(viewModel, context.owner(), context.notifier()::warning);
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
