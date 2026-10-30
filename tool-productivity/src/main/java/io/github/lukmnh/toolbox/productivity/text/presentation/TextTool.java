package io.github.lukmnh.toolbox.productivity.text.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.productivity.shared.presentation.ProductivityToolIds;
import java.nio.file.Path;
import java.util.Set;
import javafx.scene.Node;

/** Text Tools: statistik teks live, case converter, rapikan baris, undo, copy & download {@code .txt}. */
public final class TextTool implements Tool {

    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt");

    private final TextUseCases useCases;
    private TextViewModel viewModel;

    public TextTool(TextUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return ProductivityToolIds.TEXT;
    }

    @Override
    public String name() {
        return Messages.get("productivity.text.name");
    }

    @Override
    public String description() {
        return Messages.get("productivity.text.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.PRODUCTIVITY;
    }

    @Override
    public Set<String> openableExtensions() {
        return TEXT_EXTENSIONS;
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new TextViewModel(useCases, context, new TempFiles());
        return TextView.create(viewModel, context);
    }

    @Override
    public void openFile(Path file) {
        if (viewModel != null) {
            viewModel.loadFile(file);
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
