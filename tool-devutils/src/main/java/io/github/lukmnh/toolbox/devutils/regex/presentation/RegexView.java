package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Layout Regex Tester ala regex101: pola di atas, teks uji + highlight di kiri, tab analisis di kanan. */
final class RegexView {

    private static final double TABS_HEIGHT = 480;

    private RegexView() {}

    static Node create(RegexViewModel viewModel, ToolContext context) {
        VBox left = TestTextPane.create(viewModel, context);
        TabPane tabs = new TabPane(
                tab("devutils.regex.tab.explain", ExplainerPane.create(viewModel)),
                tab("devutils.regex.tab.matches", MatchTablePane.create(viewModel)),
                tab("devutils.regex.tab.replace", ReplacePane.create(viewModel)),
                tab("devutils.regex.tab.cheatSheet", CheatSheetPane.create(viewModel)));
        tabs.getStyleClass().addAll("underline-tabs", "regex-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefHeight(TABS_HEIGHT);
        VBox right = ToolLayout.card(tabs);
        right.getStyleClass().add("regex-tabs-card");
        VBox.setVgrow(tabs, Priority.ALWAYS);

        // Pref width 0 + grow: kedua kolom selalu sama lebar
        for (VBox column : new VBox[] {left, right}) {
            column.setPrefWidth(0);
            HBox.setHgrow(column, Priority.ALWAYS);
        }
        HBox columns = new HBox(ToolLayout.SPACING, left, right);

        VBox page = ToolLayout.page();
        page.getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.DEV_UTILS,
                                Messages.get("devutils.regex.name"),
                                Messages.get("devutils.regex.hint")),
                        PatternPane.create(viewModel),
                        columns);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    private static Tab tab(String titleKey, Node content) {
        return new Tab(Messages.get(titleKey), content);
    }
}
