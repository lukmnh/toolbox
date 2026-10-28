package io.github.lukmnh.toolbox.pdf.merge.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import io.github.lukmnh.toolbox.pdf.shared.presentation.PdfFileChooser;
import io.github.lukmnh.toolbox.pdf.shared.presentation.ToolLayout;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Layout Gabung PDF: daftar file yang bisa diurutkan + tombol aksi. */
public final class MergeView extends BorderPane {

    private static final double BUTTON_WIDTH = 120;

    private final MergeViewModel viewModel;
    private final Consumer<String> warn;
    private final ListView<PdfSource> list = new ListView<>();

    public MergeView(MergeViewModel viewModel, Window owner, Consumer<String> warn) {
        this.viewModel = viewModel;
        this.warn = warn;
        getStyleClass().add("tool-view");
        setPadding(new Insets(ToolLayout.SPACING * 2));
        setTop(ToolLayout.header(Messages.get("pdf.merge.name"), Messages.get("pdf.merge.hint")));
        setCenter(buildList());
        setRight(buildSideButtons(owner));
        setBottom(buildActions(owner));
        BorderPane.setMargin(getCenter(), new Insets(ToolLayout.SPACING, 0, ToolLayout.SPACING, 0));
        BorderPane.setMargin(getRight(), new Insets(ToolLayout.SPACING, 0, ToolLayout.SPACING, ToolLayout.SPACING));
    }

    private ListView<PdfSource> buildList() {
        list.setItems(viewModel.files());
        list.setPlaceholder(new Label(Messages.get("pdf.merge.empty")));
        list.setCellFactory(view -> new ReorderableCell());
        ToolLayout.acceptPdfDrops(list, viewModel::addFiles, warn);
        return list;
    }

    private VBox buildSideButtons(Window owner) {
        Button add = sideButton("pdf.merge.add");
        add.setOnAction(event -> viewModel.addFiles(PdfFileChooser.chooseMany(owner)));
        Button remove = sideButton("pdf.merge.remove");
        remove.setOnAction(event -> viewModel.remove(selectedIndex()));
        Button up = sideButton("pdf.merge.up");
        up.setOnAction(event -> moveSelected(-1));
        Button down = sideButton("pdf.merge.down");
        down.setOnAction(event -> moveSelected(1));

        var noSelection = list.getSelectionModel().selectedIndexProperty().lessThan(0);
        remove.disableProperty().bind(noSelection.or(viewModel.busyProperty()));
        up.disableProperty()
                .bind(noSelection.or(
                        list.getSelectionModel().selectedIndexProperty().isEqualTo(0)));
        down.disableProperty()
                .bind(noSelection.or(list.getSelectionModel()
                        .selectedIndexProperty()
                        .isEqualTo(Bindings.size(viewModel.files()).subtract(1))));
        add.disableProperty().bind(viewModel.busyProperty());
        return new VBox(8, add, remove, up, down);
    }

    private static Button sideButton(String key) {
        Button button = new Button(Messages.get(key));
        button.setPrefWidth(BUTTON_WIDTH);
        return button;
    }

    private VBox buildActions(Window owner) {
        Button merge = new Button(Messages.get("pdf.merge.run"));
        merge.getStyleClass().add("accent");
        merge.disableProperty().bind(viewModel.canMerge().not());
        merge.setOnAction(event -> viewModel.merge());

        Label summary = new Label();
        Button download = new Button(Messages.get("download.button"));
        download.setOnAction(event -> viewModel.download());
        Button saveAs = new Button(Messages.get("download.saveAs.button"));
        saveAs.setOnAction(event -> viewModel.saveAs());
        HBox downloadRow = ToolLayout.downloadButtons(download, saveAs);
        HBox resultRow = new HBox(ToolLayout.SPACING, summary, ToolLayout.spacer(), downloadRow);
        resultRow.setAlignment(Pos.CENTER_LEFT);
        resultRow.visibleProperty().bind(viewModel.resultProperty().isNotNull());
        resultRow.managedProperty().bind(resultRow.visibleProperty());
        summary.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.resultProperty().get() == null
                                ? ""
                                : viewModel.resultSummary(
                                        viewModel.resultProperty().get()),
                        viewModel.resultProperty()));

        TaskStatusBar status = new TaskStatusBar();
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));
        CheckBox removeProtection = ToolLayout.removeProtectionToggle(owner, viewModel.removeProtectionProperty());
        return new VBox(ToolLayout.SPACING, removeProtection, new HBox(ToolLayout.SPACING, merge), status, resultRow);
    }

    private int selectedIndex() {
        return list.getSelectionModel().getSelectedIndex();
    }

    private void moveSelected(int delta) {
        int from = selectedIndex();
        viewModel.move(from, from + delta);
        list.getSelectionModel().clearAndSelect(from + delta);
    }

    /** Sel yang bisa di-drag untuk mengubah urutan. Drop file dari luar diteruskan ke ListView. */
    private final class ReorderableCell extends ListCell<PdfSource> {

        private ReorderableCell() {
            setOnDragDetected(event -> {
                if (isEmpty()) {
                    return;
                }
                Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(Integer.toString(getIndex()));
                dragboard.setContent(content);
                event.consume();
            });
            setOnDragOver(event -> {
                if (event.getGestureSource() instanceof ReorderableCell) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    event.consume();
                }
            });
            setOnDragDropped(event -> {
                if (!(event.getGestureSource() instanceof ReorderableCell source)) {
                    return;
                }
                int to = isEmpty() ? viewModel.files().size() - 1 : getIndex();
                viewModel.move(source.getIndex(), to);
                list.getSelectionModel().clearAndSelect(to);
                event.setDropCompleted(true);
                event.consume();
            });
        }

        @Override
        protected void updateItem(PdfSource item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setTooltip(null);
                return;
            }
            setText((getIndex() + 1) + ".  " + item.fileName());
            setTooltip(new Tooltip(item.file().toAbsolutePath().toString()));
        }
    }
}
