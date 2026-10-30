package io.github.lukmnh.toolbox.productivity.rename.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.productivity.shared.presentation.ProductivityToolIds;
import java.util.Set;
import javafx.scene.Node;

/** Batch Rename: aturan bertingkat, preview wajib, two-phase rename + rollback, dan undo. */
public final class RenameTool implements Tool {

    private final RenameUseCases useCases;
    private RenameViewModel viewModel;

    public RenameTool(RenameUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return ProductivityToolIds.RENAME;
    }

    @Override
    public String name() {
        return Messages.get("productivity.rename.name");
    }

    @Override
    public String description() {
        return Messages.get("productivity.rename.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.PRODUCTIVITY;
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new RenameViewModel(useCases, context);
        return RenameView.create(viewModel, context);
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
