package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.regex.domain.Explanation;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexProblem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Tab Penjelasan: pohon token ber-indentasi. Baris grup diberi garis warna yang sama dengan highlight grup; hover
 * baris menyorot bagian pola terkait di preview.
 */
final class ExplainerPane {

    private static final double INDENT = 18;
    private static final int MAX_TOKEN_CHARS = 32;
    private static final String ELLIPSIS = "…";

    private final RegexViewModel viewModel;
    private final VBox rows = new VBox(2);

    private ExplainerPane(RegexViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static Node create(RegexViewModel viewModel) {
        return new ExplainerPane(viewModel).build();
    }

    private Node build() {
        rows.setPadding(new Insets(ToolLayout.GAP));
        viewModel.evaluationProperty().addListener((observable, previous, current) -> refresh(current));
        refresh(null);
        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    private void refresh(Evaluation evaluation) {
        rows.getChildren().clear();
        viewModel.hoveredSpanProperty().set(null);
        if (evaluation == null) {
            rows.getChildren().add(ToolLayout.hint(Messages.get("devutils.regex.explain.empty")));
            return;
        }
        for (RegexProblem problem : viewModel.visibleProblems()) {
            Label message = ToolLayout.inlineMessage(Tone.DANGER);
            message.setText(
                    RegexMessages.problem(problem, viewModel.flavorProperty().get()));
            rows.getChildren().add(message);
        }
        String pattern = evaluation.analysis().parsed().pattern();
        for (Explanation explanation : evaluation.analysis().explanation()) {
            rows.getChildren().add(row(explanation, pattern));
        }
    }

    private HBox row(Explanation explanation, String pattern) {
        Region bar = new Region();
        bar.getStyleClass().add("regex-group-bar");
        if (explanation.groupNumber() > 0) {
            bar.getStyleClass().add("regex-group-bar-" + MatchHighlights.colorIndex(explanation.groupNumber()));
        }
        Label token = new Label(token(pattern, explanation));
        token.getStyleClass().add("regex-token");
        token.setMinWidth(Region.USE_PREF_SIZE);
        Label text = new Label(ExplanationText.of(explanation));
        text.setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(ToolLayout.GAP, bar, token, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("explain-row");
        HBox.setMargin(bar, new Insets(0, 0, 0, explanation.depth() * INDENT));
        row.setOnMouseEntered(event -> viewModel.hoveredSpanProperty().set(explanation.span()));
        row.setOnMouseExited(event -> viewModel.hoveredSpanProperty().set(null));
        return row;
    }

    /** Potongan pola untuk baris ini; dipotong supaya grup panjang tidak memenuhi baris. */
    private static String token(String pattern, Explanation explanation) {
        int end = Math.min(explanation.span().end(), pattern.length());
        int start = Math.min(explanation.span().start(), end);
        String raw = pattern.substring(start, end);
        return raw.length() <= MAX_TOKEN_CHARS ? raw : raw.substring(0, MAX_TOKEN_CHARS) + ELLIPSIS;
    }
}
