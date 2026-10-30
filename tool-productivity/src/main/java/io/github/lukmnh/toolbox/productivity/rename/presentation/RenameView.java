package io.github.lukmnh.toolbox.productivity.rename.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Layout Batch Rename: header, kiri aturan + editor, kanan input + preview + aksi. */
final class RenameView {

    private static final double COLUMN_GAP = ToolLayout.SPACING * 1.5;

    private RenameView() {}

    static Node create(RenameViewModel viewModel, ToolContext context) {
        RulesPane rules = new RulesPane(viewModel);
        PreviewPane preview = new PreviewPane(viewModel, context.owner());
        HBox.setHgrow(preview, Priority.ALWAYS);
        HBox body = new HBox(COLUMN_GAP, rules, preview);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox page = ToolLayout.page();
        page.getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.PRODUCTIVITY,
                                Messages.get("productivity.rename.name"),
                                Messages.get("productivity.rename.hint")),
                        body);
        return page;
    }
}
