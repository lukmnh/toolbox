package io.github.lukmnh.toolbox.devutils.encoder.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Variant;
import io.github.lukmnh.toolbox.devutils.encoder.domain.TextCharset;
import java.io.File;
import java.util.List;
import java.util.Set;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Tab Base64: kartu opsi di atas, lalu area kerja mode Teks atau mode File. */
final class Base64Pane extends VBox {

    private static final double EDITOR_HEIGHT = 320;
    private static final double PREVIEW_HEIGHT = 220;
    private static final double IMAGE_PREVIEW_MAX = 360;

    private final Base64ViewModel viewModel;
    private final Window owner;
    private final Notifier notifier;

    Base64Pane(Base64ViewModel viewModel, Window owner, Notifier notifier) {
        super(ToolLayout.SPACING);
        this.viewModel = viewModel;
        this.owner = owner;
        this.notifier = notifier;
        Node textSection = buildTextSection();
        Node fileSection = buildFileSection();
        bindVisible(textSection, viewModel.modeProperty().isEqualTo(InputMode.TEXT));
        bindVisible(fileSection, viewModel.modeProperty().isEqualTo(InputMode.FILE));
        getChildren().addAll(buildOptions(), textSection, fileSection);
    }

    // ---------- Opsi ----------

    private VBox buildOptions() {
        Button swap = ToolLayout.button(Messages.get("devutils.encoder.swap"), viewModel::swap);
        bindVisible(swap, viewModel.modeProperty().isEqualTo(InputMode.TEXT));
        HBox primary = ToolLayout.row(
                ToolLayout.segmented(viewModel.modeProperty(), List.of(InputMode.values()), Base64Pane::modeLabel),
                ToolLayout.segmented(
                        viewModel.directionProperty(), List.of(Direction.values()), Base64Pane::directionLabel),
                ToolLayout.spacer(),
                swap);

        HBox variants = ToolLayout.segmented(
                viewModel.variantProperty(), List.of(Base64Variant.values()), Base64Pane::variantLabel);
        variants.disableProperty().bind(viewModel.encodeOptionsDisabled());
        CheckBox padding = new CheckBox(Messages.get("devutils.encoder.padding"));
        padding.selectedProperty().bindBidirectional(viewModel.paddingProperty());
        padding.disableProperty().bind(viewModel.encodeOptionsDisabled());

        ChoiceBox<TextCharset> charset = new ChoiceBox<>();
        charset.getItems().setAll(TextCharset.values());
        charset.setConverter(ToolLayout.labels(TextCharset::displayName));
        charset.valueProperty().bindBidirectional(viewModel.charsetProperty());
        VBox charsetBox = ToolLayout.labeled(Messages.get("devutils.encoder.charset"), charset);
        bindVisible(charsetBox, viewModel.modeProperty().isEqualTo(InputMode.TEXT));

        CheckBox dataUri = new CheckBox(Messages.get("devutils.encoder.dataUri"));
        dataUri.selectedProperty().bindBidirectional(viewModel.dataUriProperty());
        bindVisible(
                dataUri,
                viewModel
                        .modeProperty()
                        .isEqualTo(InputMode.FILE)
                        .and(viewModel.directionProperty().isEqualTo(Direction.ENCODE)));

        HBox secondary = ToolLayout.row(
                ToolLayout.labeled(Messages.get("devutils.encoder.variant"), variants),
                charsetBox,
                ToolLayout.labeled("", padding),
                ToolLayout.labeled("", dataUri));
        secondary.setSpacing(ToolLayout.SPACING * 2);
        return ToolLayout.card(primary, secondary);
    }

    // ---------- Mode Teks ----------

    private VBox buildTextSection() {
        TextArea input = ToolLayout.codeArea(true);
        input.setPromptText(Messages.get("devutils.encoder.input.prompt"));
        input.textProperty().bindBidirectional(viewModel.inputProperty());
        TextArea output = ToolLayout.codeArea(false);
        output.textProperty().bind(viewModel.outputProperty());

        Button copy = ClipboardActions.copyButton(viewModel.outputProperty()::get, notifier);
        copy.disableProperty().bind(viewModel.outputProperty().isEmpty());
        SplitPane editors = new SplitPane(
                editorCard(Messages.get("devutils.encoder.input"), input, List.of()),
                editorCard(Messages.get("devutils.encoder.output"), output, List.of(copy)));
        editors.getStyleClass().add("card-split");
        editors.setPrefHeight(EDITOR_HEIGHT);
        VBox.setVgrow(editors, Priority.ALWAYS);

        Label error = ToolLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.textErrorProperty());
        Label stats = ToolLayout.statText();
        stats.textProperty().bind(viewModel.statsProperty());
        HBox footer = ToolLayout.row(stats, ToolLayout.spacer(), ClipboardActions.clearButton(notifier));
        return new VBox(ToolLayout.SPACING, editors, error, footer);
    }

    private static VBox editorCard(String title, TextArea area, List<Node> trailing) {
        VBox.setVgrow(area, Priority.ALWAYS);
        VBox card = ToolLayout.card(title, trailing, area);
        card.setMinWidth(0);
        return card;
    }

    // ---------- Mode File ----------

    private VBox buildFileSection() {
        Button run = ToolLayout.accentButton("", viewModel::runFile);
        run.textProperty()
                .bind(Bindings.when(viewModel.directionProperty().isEqualTo(Direction.ENCODE))
                        .then(Messages.get("devutils.encoder.file.runEncode"))
                        .otherwise(Messages.get("devutils.encoder.file.runDecode")));
        run.disableProperty().bind(viewModel.canRunFile().not());

        TaskStatusBar status = new TaskStatusBar();
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));
        Label error = ToolLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.fileErrorProperty());

        VBox section = new VBox(
                ToolLayout.SPACING,
                buildSourceCard(),
                ToolLayout.row(ToolLayout.spacer(), run),
                status,
                error,
                buildResultCard());
        ToolLayout.acceptDrops(section, Set.of(), files -> viewModel.selectFile(files.getFirst()), notifier::warning);
        return section;
    }

    private VBox buildSourceCard() {
        VBox drop = ToolLayout.dropZone(
                Messages.get("devutils.encoder.file.drop"),
                Messages.get("devutils.encoder.file.choose"),
                this::chooseFile);
        bindVisible(drop, viewModel.sourceFileProperty().isNull());

        Label info = new Label();
        info.getStyleClass().add("file-info");
        info.textProperty().bind(viewModel.sourceInfoProperty());
        HBox selected = ToolLayout.row(
                ToolLayout.badge(Messages.get("devutils.encoder.file.selected"), Tone.INFO),
                info,
                ToolLayout.spacer(),
                ToolLayout.button(Messages.get("devutils.encoder.file.change"), this::chooseFile),
                ToolLayout.button(Messages.get("devutils.encoder.file.clear"), viewModel::clearFile));
        bindVisible(selected, viewModel.sourceFileProperty().isNotNull());

        TextArea pasted = ToolLayout.codeArea(true);
        pasted.setPromptText(Messages.get("devutils.encoder.file.paste.prompt"));
        pasted.setPrefRowCount(5);
        pasted.textProperty().bindBidirectional(viewModel.pastedBase64Property());
        VBox pasteBox = ToolLayout.labeled(Messages.get("devutils.encoder.file.paste"), pasted);
        bindVisible(
                pasteBox,
                viewModel
                        .directionProperty()
                        .isEqualTo(Direction.DECODE)
                        .and(viewModel.sourceFileProperty().isNull()));
        return ToolLayout.card(drop, selected, pasteBox);
    }

    private VBox buildResultCard() {
        Label summary = new Label();
        summary.setWrapText(true);
        summary.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.fileResultProperty().get() == null
                                ? ""
                                : viewModel.resultSummary(
                                        viewModel.fileResultProperty().get()),
                        viewModel.fileResultProperty()));

        TextArea preview = ToolLayout.codeArea(false);
        preview.setPrefHeight(PREVIEW_HEIGHT);
        Label truncated = ToolLayout.inlineMessage(Tone.NEUTRAL);
        ImageView image = new ImageView();
        image.setPreserveRatio(true);
        image.setFitWidth(IMAGE_PREVIEW_MAX);
        image.setFitHeight(IMAGE_PREVIEW_MAX);
        Label imageNote = ToolLayout.inlineMessage(Tone.WARNING);
        StackPane previewHost = new StackPane(preview, image);
        viewModel
                .fileResultProperty()
                .addListener(
                        (observable, previous, result) -> showPreview(result, preview, truncated, image, imageNote));
        showPreview(null, preview, truncated, image, imageNote);

        Button copy = ToolLayout.button(Messages.get("common.copy"), viewModel::copyFileResult);
        copy.setTooltip(new Tooltip(Messages.get("devutils.encoder.file.copy.tooltip")));
        copy.disableProperty().bind(viewModel.canCopyFileResult().not());
        Button download = ToolLayout.accentButton(Messages.get("download.button"), viewModel::download);
        Button saveAs = ToolLayout.button(Messages.get("download.saveAs.button"), viewModel::saveAs);
        HBox actions = ToolLayout.row(
                summary, ToolLayout.spacer(), copy, ClipboardActions.clearButton(notifier), saveAs, download);

        VBox card = ToolLayout.card(
                Messages.get("devutils.encoder.file.result"), List.of(), actions, previewHost, truncated, imageNote);
        bindVisible(card, viewModel.fileResultProperty().isNotNull());
        return card;
    }

    private static void showPreview(
            FileResult result, TextArea preview, Label truncated, ImageView image, Label imageNote) {
        preview.clear();
        truncated.setText("");
        image.setImage(null);
        imageNote.setText("");
        preview.setVisible(false);
        image.setVisible(false);
        switch (result) {
            case null -> {
                return;
            }
            case FileResult.Encoded encoded -> {
                preview.setVisible(true);
                preview.setText(encoded.preview().text());
                truncated.setText(
                        encoded.preview().truncated() ? Messages.get("devutils.encoder.file.preview.truncated") : "");
            }
            case FileResult.Decoded decoded -> showImage(decoded, image, imageNote);
        }
    }

    private static void showImage(FileResult.Decoded decoded, ImageView image, Label imageNote) {
        switch (decoded.image()) {
            case FileResult.ImagePreview.None none ->
                imageNote.setText(Messages.get("devutils.encoder.file.noPreview"));
            case FileResult.ImagePreview.TooLarge tooLarge ->
                imageNote.setText(Messages.format(
                        "devutils.encoder.file.imageTooLarge",
                        tooLarge.size().width(),
                        tooLarge.size().height()));
            case FileResult.ImagePreview.Allowed allowed -> {
                // Load di background; ukuran sudah dicek dari header sebelum piksel di-decode
                image.setImage(new Image(
                        decoded.output().toUri().toString(), IMAGE_PREVIEW_MAX, IMAGE_PREVIEW_MAX, true, true, true));
                image.setVisible(true);
            }
        }
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("devutils.encoder.file.chooser.title"));
        if (viewModel.directionProperty().get() == Direction.DECODE) {
            chooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter(
                            Messages.get("devutils.encoder.file.chooser.filter"), "*.txt", "*.b64", "*.TXT", "*.B64"));
        }
        File chosen = chooser.showOpenDialog(owner);
        if (chosen != null) {
            viewModel.selectFile(chosen.toPath());
        }
    }

    private static void bindVisible(Node node, BooleanBinding condition) {
        node.visibleProperty().bind(condition);
        ToolLayout.collapseWhenHidden(node);
    }

    private static String modeLabel(InputMode mode) {
        return switch (mode) {
            case TEXT -> Messages.get("devutils.encoder.mode.text");
            case FILE -> Messages.get("devutils.encoder.mode.file");
        };
    }

    static String directionLabel(Direction direction) {
        return switch (direction) {
            case ENCODE -> Messages.get("devutils.encoder.encode");
            case DECODE -> Messages.get("devutils.encoder.decode");
        };
    }

    private static String variantLabel(Base64Variant variant) {
        return switch (variant) {
            case STANDARD -> Messages.get("devutils.encoder.variant.standard");
            case URL_SAFE -> Messages.get("devutils.encoder.variant.urlSafe");
            case MIME -> Messages.get("devutils.encoder.variant.mime");
        };
    }
}
