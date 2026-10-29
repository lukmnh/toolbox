package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlag;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexFlavor;
import io.github.lukmnh.toolbox.devutils.regex.domain.Span;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevLayout;
import io.github.lukmnh.toolbox.devutils.shared.presentation.Tone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.collections.SetChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Card pola: pilihan flavor, field pola dengan delimiter, chip flag, preview + penanda error. */
final class PatternPane {

    /** Delimiter literal regex JS/PHP; sintaks bahasa, bukan teks UI. */
    private static final String DELIMITER = "/";

    private static final String CARET = "^";

    private final RegexViewModel viewModel;
    private final FlowPane flagChips = new FlowPane(DevLayout.GAP, DevLayout.GAP);
    private final Label accuracy = DevLayout.badge("", Tone.SUCCESS);
    private final HighlightedText preview = new HighlightedText("regex-pattern-preview");

    private PatternPane(RegexViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static VBox create(RegexViewModel viewModel) {
        return new PatternPane(viewModel).build();
    }

    private VBox build() {
        viewModel.flavorProperty().addListener((observable, previous, current) -> refreshFlavor(current));
        viewModel.flags().addListener((SetChangeListener<RegexFlag>) change -> rebuildFlagChips());
        refreshFlavor(viewModel.flavorProperty().get());

        Label error = DevLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.patternErrorProperty());
        return DevLayout.card(
                Messages.get("devutils.regex.pattern"),
                List.of(accuracy, copyCodeButton()),
                DevLayout.segmented(
                        viewModel.flavorProperty(), Arrays.asList(RegexFlavor.values()), RegexMessages::flavorName),
                patternRow(),
                flagChips,
                previewBox(),
                error);
    }

    private Button copyCodeButton() {
        Button button = DevLayout.button("", viewModel::copyCode);
        button.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> Messages.get("devutils.regex.copyCode."
                                + viewModel.flavorProperty().get().name()),
                        viewModel.flavorProperty()));
        button.disableProperty().bind(viewModel.patternProperty().isEmpty());
        return button;
    }

    private HBox patternRow() {
        TextField field = DevLayout.codeField();
        field.setPromptText(Messages.get("devutils.regex.pattern.prompt"));
        field.textProperty().bindBidirectional(viewModel.patternProperty());
        HBox.setHgrow(field, Priority.ALWAYS);

        Label open = delimiter();
        open.setText(DELIMITER);
        Label close = delimiter();
        close.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> DELIMITER
                                + CodeLiteral.flagLetters(
                                        viewModel.flavorProperty().get(), Set.copyOf(viewModel.flags())),
                        viewModel.flavorProperty(),
                        viewModel.flags()));
        return DevLayout.row(open, field, close);
    }

    /** Delimiter hanya relevan untuk flavor yang menulis pola sebagai {@code /re/flags} (JS dan PHP). */
    private Label delimiter() {
        Label label = new Label();
        label.getStyleClass().add("regex-delimiter");
        label.visibleProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> viewModel.flavorProperty().get().translated(), viewModel.flavorProperty()));
        DevLayout.collapseWhenHidden(label);
        return label;
    }

    private VBox previewBox() {
        Label caret = new Label();
        caret.getStyleClass().add("regex-caret");
        caret.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.errorPositionProperty().get() < 0
                                ? ""
                                : " ".repeat(viewModel.errorPositionProperty().get()) + CARET,
                        viewModel.errorPositionProperty()));
        Observable[] sources = {
            viewModel.patternProperty(), viewModel.hoveredSpanProperty(), viewModel.errorPositionProperty()
        };
        for (Observable source : sources) {
            source.addListener(observable -> refreshPreview());
        }
        VBox box = new VBox(0, preview, caret);
        box.getStyleClass().add("regex-preview-box");
        // Preview hanya muncul saat ada yang perlu ditunjuk: bagian yang di-hover atau posisi error
        box.visibleProperty()
                .bind(viewModel
                        .hoveredSpanProperty()
                        .isNotNull()
                        .or(viewModel.errorPositionProperty().greaterThanOrEqualTo(0)));
        DevLayout.collapseWhenHidden(box);
        return box;
    }

    private void refreshPreview() {
        String pattern = viewModel.patternProperty().get();
        List<HighlightRange> ranges = new ArrayList<>();
        Span hovered = viewModel.hoveredSpanProperty().get();
        if (hovered != null && hovered.end() <= pattern.length()) {
            ranges.add(new HighlightRange(hovered.start(), hovered.end(), "regex-hl-hover"));
        }
        int error = viewModel.errorPositionProperty().get();
        if (error >= 0 && error < pattern.length()) {
            ranges.add(new HighlightRange(error, error + 1, "regex-hl-error"));
        }
        preview.setContent(pattern, ranges);
    }

    private void refreshFlavor(RegexFlavor flavor) {
        boolean translated = flavor.translated();
        DevLayout.update(
                accuracy,
                Messages.get(translated ? "devutils.regex.accuracy.translated" : "devutils.regex.accuracy.native"),
                translated ? Tone.INFO : Tone.SUCCESS);
        accuracy.setTooltip(new Tooltip(Messages.get("devutils.regex.accuracy." + flavor.name() + ".tooltip")));
        rebuildFlagChips();
    }

    private void rebuildFlagChips() {
        flagChips.getChildren().clear();
        Label caption = new Label(Messages.get("devutils.regex.flags"));
        caption.getStyleClass().add("field-label");
        flagChips.getChildren().add(caption);
        for (RegexFlag flag : viewModel.flavorProperty().get().flags()) {
            ToggleButton chip = new ToggleButton(flag.token());
            chip.getStyleClass().add("regex-flag-chip");
            chip.setSelected(viewModel.flags().contains(flag));
            chip.setTooltip(new Tooltip(RegexMessages.flagTooltip(flag)));
            chip.setOnAction(event -> viewModel.toggleFlag(flag, chip.isSelected()));
            flagChips.getChildren().add(chip);
        }
    }
}
