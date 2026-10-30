package io.github.lukmnh.toolbox.devutils.qr.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevToolIds;
import java.util.Set;
import javafx.scene.Node;

/** QR Code Generator: teks, URL promo, atau vCard menjadi QR dengan preview live dan download PNG. */
public final class QrTool implements Tool {

    private final QrUseCases useCases;
    private QrViewModel viewModel;

    public QrTool(QrUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return DevToolIds.QR;
    }

    @Override
    public String name() {
        return Messages.get("devutils.qr.name");
    }

    @Override
    public String description() {
        return Messages.get("devutils.qr.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.DEV_UTILS;
    }

    /** Input diketik, bukan file. */
    @Override
    public String iconLiteral() {
        return "remixmz-qr-code-line";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        viewModel = new QrViewModel(useCases, context, new TempFiles());
        return QrView.create(viewModel);
    }

    @Override
    public void dispose() {
        if (viewModel != null) {
            viewModel.dispose();
            viewModel = null;
        }
    }
}
