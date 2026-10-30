package io.github.lukmnh.toolbox.devutils.checksum.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Layout Checksum: header, tab Generate/Verifikasi, dan progress + Batal di bawah. */
final class ChecksumView {

    private ChecksumView() {}

    static Node create(GenerateViewModel generate, VerifyViewModel verify, ToolContext context) {
        TabPane tabs = new TabPane(
                tab(
                        Messages.get("devutils.checksum.tab.generate"),
                        new GeneratePane(generate, context.owner(), context.notifier())),
                tab(
                        Messages.get("devutils.checksum.tab.verify"),
                        new VerifyPane(verify, context.owner(), context.notifier())));
        tabs.getStyleClass().add("underline-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        // Satu status bar untuk kedua tab: menampilkan task yang terakhir dimulai
        TaskStatusBar status = new TaskStatusBar();
        generate.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));
        verify.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));

        VBox page = ToolLayout.page();
        page.getChildren()
                .addAll(
                        ToolLayout.row(
                                ToolLayout.header(
                                        ToolCategory.DEV_UTILS,
                                        Messages.get("devutils.checksum.name"),
                                        Messages.get("devutils.checksum.hint")),
                                ToolLayout.spacer(),
                                ClipboardActions.clearButton(context.notifier())),
                        tabs,
                        status);
        return page;
    }

    private static Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }
}
