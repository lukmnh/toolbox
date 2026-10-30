package io.github.lukmnh.toolbox.devutils.cron.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevToolIds;
import java.util.Set;
import javafx.scene.Node;

/** Cron Builder & Explainer multi-target: Unix, Spring, Quarkus/Quartz, Go, Laravel. */
public final class CronTool implements Tool {

    private final CronUseCases useCases;
    private CronViewModel viewModel;

    public CronTool(CronUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return DevToolIds.CRON;
    }

    @Override
    public String name() {
        return Messages.get("devutils.cron.name");
    }

    @Override
    public String description() {
        return Messages.get("devutils.cron.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.DEV_UTILS;
    }

    @Override
    public String iconLiteral() {
        return "remixmz-time-line";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new CronViewModel(useCases, context);
        return CronView.create(viewModel);
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
