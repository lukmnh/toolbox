package io.github.lukmnh.toolbox.pdf.compress.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.pdf.compress.application.CompressPdfUseCase;
import io.github.lukmnh.toolbox.pdf.shared.application.InspectPdfUseCase;
import java.util.Set;
import javafx.scene.Node;

public final class CompressTool implements Tool {

    private final CompressPdfUseCase compressUseCase;
    private final InspectPdfUseCase inspectUseCase;
    private CompressViewModel viewModel;

    public CompressTool(CompressPdfUseCase compressUseCase, InspectPdfUseCase inspectUseCase) {
        this.compressUseCase = compressUseCase;
        this.inspectUseCase = inspectUseCase;
    }

    @Override
    public String id() {
        return "pdf.compress";
    }

    @Override
    public String name() {
        return Messages.get("pdf.compress.name");
    }

    @Override
    public String description() {
        return Messages.get("pdf.compress.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.PDF;
    }

    @Override
    public String iconLiteral() {
        return "remixal-file-zip-line";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new CompressViewModel(compressUseCase, inspectUseCase, context, new TempFiles());
        return new CompressView(viewModel, context.owner(), context.notifier()::warning);
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
