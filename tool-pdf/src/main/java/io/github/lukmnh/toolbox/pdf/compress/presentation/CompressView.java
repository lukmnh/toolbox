package io.github.lukmnh.toolbox.pdf.compress.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressionPreset;
import io.github.lukmnh.toolbox.pdf.shared.presentation.PdfControls;
import io.github.lukmnh.toolbox.pdf.shared.presentation.PdfFileChooser;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Layout Kompres PDF. */
public final class CompressView extends VBox {

    private final CompressViewModel viewModel;

    public CompressView(CompressViewModel viewModel, Window owner, Consumer<String> warn) {
        super(ToolLayout.SPACING * 1.5);
        this.viewModel = viewModel;
        getStyleClass().add("tool-view");
        setPadding(new Insets(ToolLayout.SPACING * 2));

        VBox dropZone = PdfControls.dropZone(
                Messages.get("pdf.drop.single"),
                () -> PdfFileChooser.chooseOne(owner).ifPresent(viewModel::openFile));
        PdfControls.acceptPdfDrops(this, files -> viewModel.openFile(files.getFirst()), warn);

        getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.PDF, Messages.get("pdf.compress.name"), Messages.get("pdf.compress.hint")),
                        dropZone,
                        infoLabel(),
                        buildPresets(),
                        buildActions(owner));
    }

    private Label infoLabel() {
        Label label = new Label();
        label.getStyleClass().add("file-info");
        label.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.infoProperty().get() == null
                                ? Messages.get("pdf.noFile")
                                : viewModel.infoSummary(viewModel.infoProperty().get()),
                        viewModel.infoProperty()));
        return label;
    }

    private VBox buildPresets() {
        ToggleGroup group = new ToggleGroup();
        VBox presets = new VBox(8, new Label(Messages.get("pdf.compress.preset.label")));
        for (CompressionPreset preset : CompressionPreset.values()) {
            RadioButton option = new RadioButton(CompressViewModel.presetLabel(preset));
            option.setToggleGroup(group);
            option.setUserData(preset);
            option.setSelected(preset == viewModel.presetProperty().get());
            presets.getChildren().add(option);
        }
        group.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected != null) {
                viewModel.presetProperty().set((CompressionPreset) selected.getUserData());
            }
        });
        presets.disableProperty().bind(viewModel.infoProperty().isNull());
        return presets;
    }

    private VBox buildActions(Window owner) {
        Button compress = new Button(Messages.get("pdf.compress.run"));
        compress.getStyleClass().add("accent");
        compress.disableProperty().bind(viewModel.canCompress().not());
        compress.setOnAction(event -> viewModel.compress());

        TaskStatusBar status = new TaskStatusBar();
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));

        Label summary = new Label();
        summary.setWrapText(true);
        summary.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.resultProperty().get() == null
                                ? ""
                                : viewModel.resultSummary(
                                        viewModel.resultProperty().get()),
                        viewModel.resultProperty()));
        Button download = new Button(Messages.get("download.button"));
        download.setOnAction(event -> viewModel.download());
        Button saveAs = new Button(Messages.get("download.saveAs.button"));
        saveAs.setOnAction(event -> viewModel.saveAs());
        HBox resultRow = new HBox(
                ToolLayout.SPACING, summary, ToolLayout.spacer(), ToolLayout.downloadButtons(download, saveAs));
        resultRow.setAlignment(Pos.CENTER_LEFT);
        resultRow.visibleProperty().bind(viewModel.resultProperty().isNotNull());
        resultRow.managedProperty().bind(resultRow.visibleProperty());

        CheckBox removeProtection = PdfControls.removeProtectionToggle(owner, viewModel.removeProtectionProperty());
        return new VBox(ToolLayout.SPACING, removeProtection, compress, status, resultRow);
    }
}
