package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.regex.application.RegexRun;
import io.github.lukmnh.toolbox.devutils.regex.application.RunRegexUseCase;
import io.github.lukmnh.toolbox.devutils.regex.domain.MatchScan;
import java.io.File;
import java.util.List;
import java.util.Set;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Kolom kiri: teks uji (paste/upload) dan panel highlight match per grup. */
final class TestTextPane {

    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "log");
    private static final double EDITOR_HEIGHT = 170;
    private static final double HIGHLIGHT_HEIGHT = 230;

    private final RegexViewModel viewModel;
    private final ToolContext context;
    private final Label countBadge = ToolLayout.badge("", Tone.NEUTRAL);
    private final HighlightedText highlight = new HighlightedText("regex-highlight-text");
    private final Label truncatedNote = ToolLayout.statText();
    private final Label emptyHint = ToolLayout.hint(Messages.get("devutils.regex.highlight.empty"));

    private TestTextPane(RegexViewModel viewModel, ToolContext context) {
        this.viewModel = viewModel;
        this.context = context;
    }

    static VBox create(RegexViewModel viewModel, ToolContext context) {
        return new TestTextPane(viewModel, context).build();
    }

    private VBox build() {
        viewModel.evaluationProperty().addListener((observable, previous, current) -> refresh(current));
        countBadge.setVisible(false);
        ToolLayout.collapseWhenHidden(countBadge);
        VBox column = new VBox(ToolLayout.SPACING, testCard(), highlightCard());
        refresh(null);
        return column;
    }

    private VBox testCard() {
        TextArea area = ToolLayout.codeArea(true);
        area.setPrefHeight(EDITOR_HEIGHT);
        area.setPromptText(Messages.get("devutils.regex.test.prompt"));
        area.textProperty().bindBidirectional(viewModel.testTextProperty());
        Button upload = ToolLayout.button(Messages.get("devutils.regex.test.upload"), this::chooseFile);
        Label warning = ToolLayout.inlineMessage(Tone.WARNING);
        warning.textProperty().bind(viewModel.runWarningProperty());

        VBox card = ToolLayout.card(Messages.get("devutils.regex.test"), List.of(countBadge, upload), area, warning);
        ToolLayout.acceptDrops(
                card,
                TEXT_EXTENSIONS,
                files -> viewModel.loadFile(files.getFirst()),
                names -> context.notifier().warning(Messages.format("devutils.drop.rejected", names)));
        return card;
    }

    private VBox highlightCard() {
        ScrollPane scroll = new ScrollPane(highlight);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(HIGHLIGHT_HEIGHT);
        scroll.getStyleClass().add("regex-highlight-scroll");
        ToolLayout.collapseWhenHidden(truncatedNote);
        ToolLayout.collapseWhenHidden(emptyHint);
        return ToolLayout.card(Messages.get("devutils.regex.highlight"), List.of(), emptyHint, scroll, truncatedNote);
    }

    private void refresh(Evaluation evaluation) {
        RegexRun run = evaluation == null ? null : evaluation.run().orElse(null);
        String text = evaluation == null ? "" : evaluation.text();
        emptyHint.setVisible(run == null || text.isEmpty());
        if (run == null) {
            countBadge.setVisible(false);
            highlight.setContent("", List.of());
            truncatedNote.setVisible(false);
            return;
        }
        String visible = MatchHighlights.visibleText(text);
        highlight.setContent(visible, MatchHighlights.of(run.scan().matches(), visible.length()));
        showCount(run.scan());
        showTruncation(text, visible, run.scan());
    }

    private void showCount(MatchScan scan) {
        String count = RegexMessages.number(scan.totalCount());
        String text = scan.countComplete()
                ? Messages.format("devutils.regex.matchCount", count)
                : Messages.format("devutils.regex.matchCount.atLeast", count);
        ToolLayout.update(countBadge, text, scan.totalCount() == 0 ? Tone.NEUTRAL : Tone.INFO);
        countBadge.setVisible(true);
    }

    private void showTruncation(String text, String visible, MatchScan scan) {
        boolean textCut = visible.length() < text.length();
        boolean matchesCut = scan.totalCount() > scan.matches().size()
                && scan.matches().size() >= RunRegexUseCase.MAX_DETAILED_MATCHES;
        truncatedNote.setVisible(textCut || matchesCut);
        truncatedNote.setText(Messages.format(
                "devutils.regex.highlight.truncated",
                RegexMessages.number(MatchHighlights.MAX_CHARS / 1_000),
                RegexMessages.number(RunRegexUseCase.MAX_DETAILED_MATCHES)));
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("devutils.regex.test.chooser.title"));
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(
                        Messages.get("devutils.regex.test.chooser.filter"), "*.txt", "*.log", "*.TXT", "*.LOG"));
        File chosen = chooser.showOpenDialog(context.owner());
        if (chosen != null) {
            viewModel.loadFile(chosen.toPath());
        }
    }
}
