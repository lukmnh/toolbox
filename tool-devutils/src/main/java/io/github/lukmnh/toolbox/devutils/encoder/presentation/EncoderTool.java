package io.github.lukmnh.toolbox.devutils.encoder.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevToolIds;
import java.util.Set;
import javafx.scene.Node;

/** Encoder / Decoder: Base64 (teks & file) dan URL (encode/decode, parser query string). */
public final class EncoderTool implements Tool {

    private final EncoderUseCases useCases;
    private Base64ViewModel base64ViewModel;
    private UrlViewModel urlViewModel;

    public EncoderTool(EncoderUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return DevToolIds.ENCODER;
    }

    @Override
    public String name() {
        return Messages.get("devutils.encoder.name");
    }

    @Override
    public String description() {
        return Messages.get("devutils.encoder.description");
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
        base64ViewModel = new Base64ViewModel(useCases, context, new TempFiles());
        urlViewModel = new UrlViewModel(context.notifier());
        return EncoderView.create(base64ViewModel, urlViewModel, context);
    }

    @Override
    public void dispose() {
        if (base64ViewModel != null) {
            base64ViewModel.dispose();
            base64ViewModel = null;
        }
        if (urlViewModel != null) {
            urlViewModel.dispose();
            urlViewModel = null;
        }
    }
}
