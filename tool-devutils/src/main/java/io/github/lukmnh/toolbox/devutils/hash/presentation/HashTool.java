package io.github.lukmnh.toolbox.devutils.hash.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevToolIds;
import java.util.Set;
import javafx.scene.Node;

/** Hash Generator: hash/HMAC teks, plus generate random secret, key pair PEM, dan UUID tanpa input. */
public final class HashTool implements Tool {

    private final HashUseCases useCases;
    private HashViewModel viewModel;
    private GenerateViewModel generateViewModel;

    public HashTool(HashUseCases useCases) {
        this.useCases = useCases;
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
    public String iconLiteral() {
        return "remixal-hashtag";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new HashViewModel(useCases.hash(), useCases.hmac(), context);
        generateViewModel = new GenerateViewModel(useCases, context, new TempFiles());
        return new HashView(viewModel, generateViewModel, context.notifier());
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
        if (generateViewModel != null) {
            generateViewModel.dispose();
            generateViewModel = null;
        }
    }
}
