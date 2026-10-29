package io.github.lukmnh.toolbox.devutils.sql.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevToolIds;
import java.util.Set;
import javafx.scene.Node;

/** SQL Formatter: format/minify multi-dialect, download {@code .formatted.sql} / {@code .min.sql}. */
public final class SqlTool implements Tool {

    private final SqlUseCases useCases;
    private SqlViewModel viewModel;

    public SqlTool(SqlUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return DevToolIds.SQL;
    }

    @Override
    public String name() {
        return Messages.get("devutils.sql.name");
    }

    @Override
    public String description() {
        return Messages.get("devutils.sql.description");
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
        viewModel = new SqlViewModel(useCases, context, new TempFiles());
        return SqlView.create(viewModel, context);
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
