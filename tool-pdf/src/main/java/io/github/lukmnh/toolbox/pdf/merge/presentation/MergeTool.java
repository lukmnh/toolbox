package io.github.lukmnh.toolbox.pdf.merge.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.pdf.merge.application.MergePdfUseCase;
import java.util.Set;
import javafx.scene.Node;

public final class MergeTool implements Tool {

    private final MergePdfUseCase mergeUseCase;
    private MergeViewModel viewModel;

    public MergeTool(MergePdfUseCase mergeUseCase) {
        this.mergeUseCase = mergeUseCase;
    }

    @Override
    public String id() {
        return "pdf.merge";
    }

    @Override
    public String name() {
        return Messages.get("pdf.merge.name");
    }

    @Override
    public String description() {
        return Messages.get("pdf.merge.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.PDF;
    }

    @Override
    public String iconLiteral() {
        return "remixal-file-copy-2-line";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new MergeViewModel(mergeUseCase, context, new TempFiles());
        return new MergeView(viewModel, context.owner(), context.notifier()::warning);
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
