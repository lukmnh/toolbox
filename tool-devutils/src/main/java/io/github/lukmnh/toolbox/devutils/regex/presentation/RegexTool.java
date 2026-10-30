package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevToolIds;
import java.util.Set;
import javafx.scene.Node;

/** Regex Tester & Explainer multi-flavor: Java, JavaScript, Go, dan PHP. */
public final class RegexTool implements Tool {

    private final RegexUseCases useCases;
    private RegexViewModel viewModel;

    public RegexTool(RegexUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return DevToolIds.REGEX;
    }

    @Override
    public String name() {
        return Messages.get("devutils.regex.name");
    }

    @Override
    public String description() {
        return Messages.get("devutils.regex.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.DEV_UTILS;
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new RegexViewModel(useCases, context);
        return RegexView.create(viewModel, context);
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
