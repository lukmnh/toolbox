package io.github.lukmnh.toolbox.devutils.checksum.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevToolIds;
import java.util.Set;
import javafx.scene.Node;

/** Checksum file: generate (format {@code sha256sum}) dan verifikasi. */
public final class ChecksumTool implements Tool {

    private final ChecksumUseCases useCases;
    private GenerateViewModel generateViewModel;
    private VerifyViewModel verifyViewModel;

    public ChecksumTool(ChecksumUseCases useCases) {
        this.useCases = useCases;
    }

    @Override
    public String id() {
        return DevToolIds.CHECKSUM;
    }

    @Override
    public String name() {
        return Messages.get("devutils.checksum.name");
    }

    @Override
    public String description() {
        return Messages.get("devutils.checksum.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.DEV_UTILS;
    }

    @Override
    public String iconLiteral() {
        return "remixal-fingerprint-line";
    }

    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        generateViewModel = new GenerateViewModel(useCases, context, new TempFiles());
        verifyViewModel = new VerifyViewModel(useCases, context);
        return ChecksumView.create(generateViewModel, verifyViewModel, context);
    }

    @Override
    public void dispose() {
        if (generateViewModel != null) {
            generateViewModel.dispose();
            generateViewModel = null;
        }
        if (verifyViewModel != null) {
            verifyViewModel.dispose();
            verifyViewModel = null;
        }
    }
}
