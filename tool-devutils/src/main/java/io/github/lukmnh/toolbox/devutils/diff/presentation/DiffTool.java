package io.github.lukmnh.toolbox.devutils.diff.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevToolIds;
import java.util.Set;
import javafx.scene.Node;

/** Text Diff: side-by-side & unified, highlight per kata, export {@code .diff}. */
public final class DiffTool implements Tool {

    private final DiffUseCases useCases;
    private DiffViewModel viewModel;

    public DiffTool(DiffUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return DevToolIds.DIFF;
    }

    @Override
    public String name() {
        return Messages.get("devutils.diff.name");
    }

    @Override
    public String description() {
        return Messages.get("devutils.diff.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.DEV_UTILS;
    }

    @Override
    public String iconLiteral() {
        return "remixmz-split-cells-horizontal";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new DiffViewModel(useCases, context, new TempFiles());
        return DiffView.create(viewModel, context);
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
