package io.github.lukmnh.toolbox.devutils.jwt.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.jwt.application.DecodeJwtUseCase;
import io.github.lukmnh.toolbox.devutils.jwt.application.VerifyJwtSignatureUseCase;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevToolIds;
import java.util.Set;
import javafx.scene.Node;

public final class JwtTool implements Tool {

    private final DecodeJwtUseCase decodeUseCase;
    private final VerifyJwtSignatureUseCase verifyUseCase;
    private JwtViewModel viewModel;

    public JwtTool(DecodeJwtUseCase decodeUseCase, VerifyJwtSignatureUseCase verifyUseCase) {
        this.decodeUseCase = decodeUseCase;
        this.verifyUseCase = verifyUseCase;
    }

    @Override
    public String id() {
        return DevToolIds.JWT;
    }

    @Override
    public String name() {
        return Messages.get("devutils.jwt.name");
    }

    @Override
    public String description() {
        return Messages.get("devutils.jwt.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.DEV_UTILS;
    }

    @Override
    public String iconLiteral() {
        return "remixal-key-2-line";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new JwtViewModel(decodeUseCase, verifyUseCase, context);
        return new JwtView(viewModel, context.notifier());
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
