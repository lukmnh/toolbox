package io.github.lukmnh.toolbox.pdf.split.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.pdf.shared.application.InspectPdfUseCase;
import io.github.lukmnh.toolbox.pdf.split.application.SplitPdfUseCase;
import java.util.Set;
import javafx.scene.Node;

public final class SplitTool implements Tool {

    private final SplitPdfUseCase splitUseCase;
    private final InspectPdfUseCase inspectUseCase;
    private SplitViewModel viewModel;

    public SplitTool(SplitPdfUseCase splitUseCase, InspectPdfUseCase inspectUseCase) {
        this.splitUseCase = splitUseCase;
        this.inspectUseCase = inspectUseCase;
    }

    @Override
    public String id() {
        return "pdf.split";
    }

    @Override
    public String name() {
        return Messages.get("pdf.split.name");
    }

    @Override
    public String description() {
        return Messages.get("pdf.split.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.PDF;
    }

    @Override
    public String iconLiteral() {
        return "remixmz-scissors-cut-line";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new SplitViewModel(splitUseCase, inspectUseCase, context, new TempFiles());
        return new SplitView(viewModel, context.owner(), context.notifier()::warning);
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
