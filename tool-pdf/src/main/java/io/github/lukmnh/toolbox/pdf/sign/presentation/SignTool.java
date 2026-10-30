package io.github.lukmnh.toolbox.pdf.sign.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import java.nio.file.Path;
import java.util.Set;
import javafx.scene.Node;

/** Tool Isi & TTD; juga tujuan "Open with" untuk file {@code .pdf}. */
public final class SignTool implements Tool {

    private final SignUseCases useCases;
    private SignViewModel viewModel;

    public SignTool(SignUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return "pdf.sign";
    }

    @Override
    public String name() {
        return Messages.get("pdf.sign.name");
    }

    @Override
    public String description() {
        return Messages.get("pdf.sign.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.PDF;
    }

    @Override
    public String iconLiteral() {
        return "remixmz-quill-pen-line";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of("pdf");
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new SignViewModel(useCases, context, new TempFiles());
        viewModel.loadLastSignature();
        return new SignView(viewModel, context.owner());
    }

    @Override
    public void openFile(Path file) {
        if (viewModel != null) {
            viewModel.openFile(file);
        }
    }

    @Override
    public boolean canClose() {
        return viewModel == null || !viewModel.isDirty();
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
