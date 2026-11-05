package io.github.lukmnh.toolbox.devutils.sql.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.sql.domain.SqlSnippet;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Daftar referensi SQL (dipakai mode Cheatsheet dan Concepts): search + card per grup. Tiap snippet berisi judul,
 * kode, penjelasan, dan kapan dipakai. Grup konsep menampilkan intro di atas snippet-nya.
 */
final class SqlReferenceView {

    private final List<String> groupOrder;
    private final boolean withIntro;
    private final Notifier notifier;
    private final VBox groups = new VBox(ToolLayout.SPACING);

    private SqlReferenceView(List<String> groupOrder, boolean withIntro, Notifier notifier) {
        this.groupOrder = groupOrder;
        this.withIntro = withIntro;
        this.notifier = notifier;
    }

    static Node create(List<String> groupOrder, boolean withIntro, Notifier notifier) {
        return new SqlReferenceView(groupOrder, withIntro, notifier).build();
    }

    private Node build() {
        TextField search = new TextField();
        search.setPromptText(Messages.get("devutils.sql.search"));
        search.textProperty().addListener((observable, previous, current) -> render(current));
        render("");
        return new VBox(ToolLayout.SPACING, search, groups);
    }

    private void render(String query) {
        groups.getChildren().clear();
        for (String group : groupOrder) {
            List<SqlSnippet> matches = SqlTexts.matching(group, query);
            if (matches.isEmpty()) {
                continue;
            }
            VBox rows = new VBox(ToolLayout.SPACING);
            if (withIntro) {
                rows.getChildren().add(intro(group));
            }
            matches.forEach(snippet -> rows.getChildren().add(snippetBlock(snippet)));
            groups.getChildren().add(ToolLayout.card(SqlTexts.group(group), List.of(), rows));
        }
        if (groups.getChildren().isEmpty()) {
            groups.getChildren().add(ToolLayout.hint(Messages.get("devutils.sql.noResult")));
        }
    }

    private static Label intro(String group) {
        Label intro = ToolLayout.inlineMessage(Tone.INFO);
        intro.setText(SqlTexts.intro(group));
        intro.setWrapText(true);
        intro.setMaxWidth(Double.MAX_VALUE);
        return intro;
    }

    /** Judul, kode (+ tombol copy), lalu penjelasan dan "kapan dipakai". */
    private VBox snippetBlock(SqlSnippet snippet) {
        Label code = ToolLayout.mono(new Label(snippet.code()));
        code.getStyleClass().add("code-block");
        code.setWrapText(true);
        code.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(code, Priority.ALWAYS);

        Label title = new Label(SqlTexts.title(snippet));
        title.getStyleClass().add("snippet-title");
        title.setWrapText(true);

        Label description = new Label(SqlTexts.snippet(snippet));
        description.setWrapText(true);
        Label when = ToolLayout.hint(Messages.format("devutils.sql.when", SqlTexts.when(snippet)));

        HBox codeRow = ToolLayout.row(code, ClipboardActions.copyButton(snippet::code, notifier));
        VBox block = new VBox(ToolLayout.GAP, title, codeRow, description, when);
        block.getStyleClass().add("result-row");
        return block;
    }
}
