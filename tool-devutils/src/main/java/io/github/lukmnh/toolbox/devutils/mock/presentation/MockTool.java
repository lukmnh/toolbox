package io.github.lukmnh.toolbox.devutils.mock.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevToolIds;
import java.util.Set;
import javafx.scene.Node;

/** Mock Data Generator: data uji JSON/CSV/SQL yang reproducible lewat seed. */
public final class MockTool implements Tool {

    private final MockUseCases useCases;
    private MockViewModel viewModel;

    public MockTool(MockUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return DevToolIds.MOCK;
    }

    @Override
    public String name() {
        return Messages.get("devutils.mock.name");
    }

    @Override
    public String description() {
        return Messages.get("devutils.mock.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.DEV_UTILS;
    }

    @Override
    public String iconLiteral() {
        return "remixmz-magic-line";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new MockViewModel(useCases, context, new TempFiles());
        return MockView.create(viewModel);
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
