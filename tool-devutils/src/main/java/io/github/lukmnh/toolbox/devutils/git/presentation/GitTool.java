package io.github.lukmnh.toolbox.devutils.git.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.git.application.BuildGitCommandUseCase;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevToolIds;
import java.util.Set;
import javafx.scene.Node;

/** Git Command Builder + cheatsheet. Hanya menyusun command; tidak pernah menjalankan git. */
public final class GitTool implements Tool {

    private final BuildGitCommandUseCase buildCommand;
    private GitViewModel viewModel;

    public GitTool(BuildGitCommandUseCase buildCommand) {
        this.buildCommand = buildCommand;
    }

    @Override
    public String id() {
        return DevToolIds.GIT;
    }

    @Override
    public String name() {
        return Messages.get("devutils.git.name");
    }

    @Override
    public String description() {
        return Messages.get("devutils.git.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.DEV_UTILS;
    }

    @Override
    public String iconLiteral() {
        return "remixal-git-branch-line";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new GitViewModel(buildCommand, context);
        return GitView.create(viewModel);
    }

    /** Tidak ada task, file, atau listener eksternal; cukup lepas view model. */
    @Override
    public void dispose() {
        viewModel = null;
    }
}
