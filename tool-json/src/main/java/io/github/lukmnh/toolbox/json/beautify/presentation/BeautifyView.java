package io.github.lukmnh.toolbox.json.beautify.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.json.beautify.domain.BeautifyAction;
import io.github.lukmnh.toolbox.json.beautify.domain.Indent;
import io.github.lukmnh.toolbox.json.shared.presentation.JsonLayout;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

/** Layout JSON Beautify: input (paste/upload) di kiri, preview hasil di kanan. */
public final class BeautifyView extends VBox {

    private static final Set<String> JSON_EXTENSIONS = Set.of("json");

    private final BeautifyViewModel viewModel;
    private final Window owner;

    public BeautifyView(BeautifyViewModel viewModel, Window owner, Consumer<String> warn) {
        super(JsonLayout.SPACING);
        this.viewModel = viewModel;
        this.owner = owner;
        getStyleClass().add("tool-view");
        setPadding(new Insets(JsonLayout.SPACING * 2));
        JsonLayout.acceptDrops(
                this,
                JSON_EXTENSIONS,
                files -> viewModel.openFile(files.getFirst()),
                names -> warn.accept(Messages.format("json.beautify.drop.rejected", names)));

        SplitPane editors = new SplitPane(buildInput(), buildOutput());
        VBox.setVgrow(editors, Priority.ALWAYS);
        TaskStatusBar status = new TaskStatusBar();
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));

        getChildren()
                .addAll(
                        JsonLayout.header(Messages.get("json.beautify.name"), Messages.get("json.beautify.hint")),
                        buildFileRow(),
                        editors,
                        buildOptions(),
                        status,
                        buildError(),
                        buildResultRow());
    }

    private HBox buildFileRow() {
        Button upload = new Button(Messages.get("json.beautify.upload"));
        upload.setOnAction(event -> chooseFile());
        Button clear = new Button(Messages.get("json.beautify.file.clear"));
        clear.setOnAction(event -> viewModel.clearFile());
        clear.visibleProperty().bind(viewModel.fileProperty().isNotNull());
        JsonLayout.collapseWhenHidden(clear);
        Label info = new Label();
        info.getStyleClass().add("file-info");
        info.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.fileProperty().get() == null
                                ? Messages.get("json.beautify.file.none")
                                : viewModel.fileSummary(viewModel.fileProperty().get()),
                        viewModel.fileProperty()));
        HBox row = new HBox(JsonLayout.SPACING, upload, clear, info);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildInput() {
        TextArea input = JsonLayout.monospace(new TextArea());
        input.setPromptText(Messages.get("json.beautify.input.prompt"));
        input.textProperty().bindBidirectional(viewModel.textProperty());
        // File besar tidak dimuat ke TextArea; selama ada file, input paste dinonaktifkan
        input.disableProperty().bind(viewModel.fileProperty().isNotNull());
        return labeled(Messages.get("json.beautify.input"), input, new Label());
    }

    private VBox buildOutput() {
        TextArea output = JsonLayout.monospace(new TextArea());
        output.setEditable(false);
        output.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.previewProperty().get() == null
                                ? ""
                                : viewModel.previewProperty().get().text(),
                        viewModel.previewProperty()));
        Label truncated = new Label(Messages.get("json.beautify.preview.truncated"));
        truncated.getStyleClass().add("field-label");
        truncated
                .visibleProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> viewModel.previewProperty().get() != null
                                && viewModel.previewProperty().get().truncated(),
                        viewModel.previewProperty()));
        JsonLayout.collapseWhenHidden(truncated);
        return labeled(Messages.get("json.beautify.output"), output, truncated);
    }

    private static VBox labeled(String title, TextArea area, Label footer) {
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        VBox.setVgrow(area, Priority.ALWAYS);
        VBox box = new VBox(6, label, area, footer);
        box.setPadding(new Insets(0, 6, 0, 6));
        return box;
    }

    private HBox buildOptions() {
        ChoiceBox<Indent> indent = new ChoiceBox<>();
        indent.getItems().setAll(Indent.values());
        indent.setConverter(labels(Map.of(
                Indent.TWO_SPACES, Messages.get("json.beautify.indent.two"),
                Indent.FOUR_SPACES, Messages.get("json.beautify.indent.four"),
                Indent.TAB, Messages.get("json.beautify.indent.tab"))));
        indent.valueProperty().bindBidirectional(viewModel.indentProperty());

        CheckBox sortKeys = new CheckBox(Messages.get("json.beautify.sortKeys"));
        sortKeys.selectedProperty().bindBidirectional(viewModel.sortKeysProperty());
        sortKeys.disableProperty().bind(viewModel.sortKeysAvailable().not());
        Tooltip sortTooltip = new Tooltip();
        sortTooltip
                .textProperty()
                .bind(Bindings.when(viewModel.sortKeysAvailable())
                        .then(Messages.get("json.beautify.sortKeys.tooltip"))
                        .otherwise(Messages.get("json.beautify.sortKeys.disabled")));
        sortKeys.setTooltip(sortTooltip);
        // Tooltip tidak muncul pada kontrol disabled; tampilkan alasan sebagai label juga
        Label sortHint = new Label(Messages.get("json.beautify.sortKeys.disabled"));
        sortHint.getStyleClass().add("field-label");
        sortHint.visibleProperty().bind(viewModel.sortKeysAvailable().not());
        JsonLayout.collapseWhenHidden(sortHint);

        HBox row = new HBox(
                JsonLayout.SPACING,
                new Label(Messages.get("json.beautify.indent")),
                indent,
                sortKeys,
                sortHint,
                JsonLayout.spacer(),
                actionButton(BeautifyAction.VALIDATE, "json.beautify.validate"),
                actionButton(BeautifyAction.MINIFY, "json.beautify.minify"),
                actionButton(BeautifyAction.BEAUTIFY, "json.beautify.beautify"));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button actionButton(BeautifyAction action, String labelKey) {
        Button button = new Button(Messages.get(labelKey));
        if (action == BeautifyAction.BEAUTIFY) {
            button.getStyleClass().add("accent");
        }
        button.disableProperty().bind(viewModel.canRun().not());
        button.setOnAction(event -> viewModel.run(action));
        return button;
    }

    private VBox buildError() {
        Label header = new Label();
        header.getStyleClass().add("text-error");
        header.setWrapText(true);
        Label detail = JsonLayout.monospace(new Label());
        detail.setWrapText(false);
        header.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.errorProperty().get() == null
                                ? ""
                                : viewModel.errorHeader(
                                        viewModel.errorProperty().get()),
                        viewModel.errorProperty()));
        detail.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.errorProperty().get() == null
                                ? ""
                                : viewModel.errorDetail(
                                        viewModel.errorProperty().get()),
                        viewModel.errorProperty()));
        VBox box = new VBox(4, header, detail);
        box.visibleProperty().bind(viewModel.errorProperty().isNotNull());
        JsonLayout.collapseWhenHidden(box);
        return box;
    }

    private HBox buildResultRow() {
        Label summary = new Label();
        summary.setWrapText(true);
        summary.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.resultProperty().get() == null
                                ? ""
                                : viewModel.resultSummary(
                                        viewModel.resultProperty().get()),
                        viewModel.resultProperty()));

        Button copy = new Button(Messages.get("json.beautify.copy"));
        copy.disableProperty().bind(viewModel.canCopy().not());
        copy.setTooltip(new Tooltip(Messages.get("json.beautify.copy.tooltip")));
        copy.setOnAction(event -> viewModel.copy(BeautifyView::putOnClipboard));
        Button download = new Button(Messages.get("download.button"));
        download.setOnAction(event -> viewModel.download());
        Button saveAs = new Button(Messages.get("download.saveAs.button"));
        saveAs.setOnAction(event -> viewModel.saveAs());
        HBox buttons = new HBox(8, copy, JsonLayout.downloadButtons(download, saveAs));
        buttons.visibleProperty().bind(viewModel.hasOutput());
        JsonLayout.collapseWhenHidden(buttons);

        HBox row = new HBox(JsonLayout.SPACING, summary, JsonLayout.spacer(), buttons);
        row.setAlignment(Pos.CENTER_LEFT);
        row.visibleProperty().bind(viewModel.resultProperty().isNotNull());
        JsonLayout.collapseWhenHidden(row);
        return row;
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("json.beautify.chooser.title"));
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(Messages.get("json.chooser.filter.json"), "*.json", "*.JSON"));
        File chosen = chooser.showOpenDialog(owner);
        if (chosen != null) {
            viewModel.openFile(chosen.toPath());
        }
    }

    private static void putOnClipboard(String text) {
        Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, text));
    }

    private static <T> StringConverter<T> labels(Map<T, String> labels) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : labels.get(value);
            }

            @Override
            public T fromString(String text) {
                throw new UnsupportedOperationException("ChoiceBox tidak bisa diketik");
            }
        };
    }
}
