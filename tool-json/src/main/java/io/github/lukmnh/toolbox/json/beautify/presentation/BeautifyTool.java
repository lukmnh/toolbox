package io.github.lukmnh.toolbox.json.beautify.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.json.beautify.application.FormatJsonUseCase;
import io.github.lukmnh.toolbox.json.beautify.application.LoadJsonFileUseCase;
import io.github.lukmnh.toolbox.json.beautify.application.ReadOutputUseCase;
import io.github.lukmnh.toolbox.json.shared.presentation.JsonToolIds;
import java.util.Set;
import javafx.scene.Node;

public final class BeautifyTool implements Tool {

    private final LoadJsonFileUseCase loadUseCase;
    private final FormatJsonUseCase formatUseCase;
    private final ReadOutputUseCase readUseCase;
    private BeautifyViewModel viewModel;

    public BeautifyTool(
            LoadJsonFileUseCase loadUseCase, FormatJsonUseCase formatUseCase, ReadOutputUseCase readUseCase) {
        this.loadUseCase = loadUseCase;
        this.formatUseCase = formatUseCase;
        this.readUseCase = readUseCase;
    }

    @Override
    public String id() {
        return JsonToolIds.BEAUTIFY;
    }

    @Override
    public String name() {
        return Messages.get("json.beautify.name");
    }

    @Override
    public String description() {
        return Messages.get("json.beautify.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.JSON;
    }

    /** "Open with" untuk {@code .json} diarahkan ke JSON Viewer. */
    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new BeautifyViewModel(loadUseCase, formatUseCase, readUseCase, context, new TempFiles());
        return new BeautifyView(viewModel, context.owner(), context.notifier()::warning);
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
