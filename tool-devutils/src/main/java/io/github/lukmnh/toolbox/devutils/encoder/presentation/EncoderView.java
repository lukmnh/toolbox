package io.github.lukmnh.toolbox.devutils.encoder.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevLayout;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Layout Encoder / Decoder: header lalu dua tab (Base64, URL) yang masing-masing bisa di-scroll. */
final class EncoderView {

    private static final double TAB_CONTENT_PADDING = 4;

    private EncoderView() {}

    static VBox create(Base64ViewModel base64, UrlViewModel url, ToolContext context) {
        TabPane tabs = new TabPane(
                tab(
                        Messages.get("devutils.encoder.tab.base64"),
                        new Base64Pane(base64, context.owner(), context.notifier())),
                tab(Messages.get("devutils.encoder.tab.url"), new UrlPane(url, context.notifier())));
        tabs.getStyleClass().add("underline-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        VBox page = DevLayout.page();
        page.getChildren()
                .addAll(
                        DevLayout.header(Messages.get("devutils.encoder.name"), Messages.get("devutils.encoder.hint")),
                        tabs);
        return page;
    }

    private static Tab tab(String title, Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPadding(new Insets(DevLayout.SPACING, TAB_CONTENT_PADDING, TAB_CONTENT_PADDING, TAB_CONTENT_PADDING));
        scroll.getStyleClass().add("tool-view");
        return new Tab(title, scroll);
    }
}
