package io.github.lukmnh.toolbox.pdf.split.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.pdf.shared.presentation.PdfFileChooser;
import io.github.lukmnh.toolbox.pdf.shared.presentation.ToolLayout;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Layout Pisah PDF. */
public final class SplitView extends VBox {

    private static final double RANGE_FIELD_WIDTH = 260;

    private final SplitViewModel viewModel;

    public SplitView(SplitViewModel viewModel, Window owner, Consumer<String> warn) {
        super(ToolLayout.SPACING * 1.5);
        this.viewModel = viewModel;
        getStyleClass().add("tool-view");
        setPadding(new Insets(ToolLayout.SPACING * 2));

        VBox dropZone = ToolLayout.dropZone(
                Messages.get("pdf.drop.single"),
                () -> PdfFileChooser.chooseOne(owner).ifPresent(viewModel::openFile));
        ToolLayout.acceptPdfDrops(this, files -> viewModel.openFile(files.getFirst()), warn);

        getChildren()
                .addAll(
                        ToolLayout.header(Messages.get("pdf.split.name"), Messages.get("pdf.split.hint")),
                        dropZone,
                        infoLabel(),
                        buildOptions(),
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

    private VBox buildOptions() {
        ToggleGroup group = new ToggleGroup();
        RadioButton eachPage = new RadioButton(Messages.get(SplitChoice.EACH_PAGE.messageKey()));
        eachPage.setToggleGroup(group);
        eachPage.setUserData(SplitChoice.EACH_PAGE);
        RadioButton ranges = new RadioButton(Messages.get(SplitChoice.RANGES.messageKey()));
        ranges.setToggleGroup(group);
        ranges.setUserData(SplitChoice.RANGES);
        group.selectToggle(viewModel.modeProperty().get() == SplitChoice.EACH_PAGE ? eachPage : ranges);
        group.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected != null) {
                viewModel.modeProperty().set((SplitChoice) selected.getUserData());
            }
        });

        TextField rangeField = new TextField();
        rangeField.setPromptText(Messages.get("pdf.split.ranges.prompt"));
        rangeField.setPrefWidth(RANGE_FIELD_WIDTH);
        rangeField.textProperty().bindBidirectional(viewModel.rangeSpecificationProperty());
        rangeField.disableProperty().bind(ranges.selectedProperty().not());
        HBox rangeRow = new HBox(8, ranges, rangeField);
        rangeRow.setAlignment(Pos.CENTER_LEFT);

        VBox options = new VBox(8, eachPage, rangeRow);
        options.disableProperty().bind(viewModel.infoProperty().isNull());
        return options;
    }

    private VBox buildActions(Window owner) {
        Button split = new Button(Messages.get("pdf.split.run"));
        split.getStyleClass().add("accent");
        split.disableProperty().bind(viewModel.canSplit().not());
        split.setOnAction(event -> viewModel.split());

        TaskStatusBar status = new TaskStatusBar();
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));

        Label summary = new Label();
        summary.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.resultProperty().get() == null
                                ? ""
                                : viewModel.resultSummary(
                                        viewModel.resultProperty().get()),
                        viewModel.resultProperty()));
        Button download = new Button(Messages.get("download.button"));
        download.setOnAction(event -> viewModel.download());
        Button saveAs = new Button(Messages.get("download.saveAsFolder.button"));
        saveAs.setOnAction(event -> viewModel.saveAs());
        HBox resultRow = new HBox(
                ToolLayout.SPACING, summary, ToolLayout.spacer(), ToolLayout.downloadButtons(download, saveAs));
        resultRow.setAlignment(Pos.CENTER_LEFT);
        resultRow.visibleProperty().bind(viewModel.resultProperty().isNotNull());
        resultRow.managedProperty().bind(resultRow.visibleProperty());

        CheckBox removeProtection = ToolLayout.removeProtectionToggle(owner, viewModel.removeProtectionProperty());
        return new VBox(ToolLayout.SPACING, removeProtection, split, status, resultRow);
    }
}
