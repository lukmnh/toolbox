package io.github.lukmnh.toolbox.devutils.hash.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.hash.application.HashTextUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.HmacTextUseCase;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevToolIds;
import java.util.Set;
import javafx.scene.Node;

public final class HashTool implements Tool {

    private final HashTextUseCase hashUseCase;
    private final HmacTextUseCase hmacUseCase;
    private HashViewModel viewModel;

    public HashTool(HashTextUseCase hashUseCase, HmacTextUseCase hmacUseCase) {
        this.hashUseCase = hashUseCase;
        this.hmacUseCase = hmacUseCase;
    }

    @Override
    public String id() {
        return DevToolIds.HASH;
    }

    @Override
    public String name() {
        return Messages.get("devutils.hash.name");
    }

    @Override
    public String description() {
        return Messages.get("devutils.hash.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.DEV_UTILS;
    }

    /** Input berupa teks, bukan file. */
    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new HashViewModel(hashUseCase, hmacUseCase, context);
        return new HashView(viewModel, context.notifier());
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
