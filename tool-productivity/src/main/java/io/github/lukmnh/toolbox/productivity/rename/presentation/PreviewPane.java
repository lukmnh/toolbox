package io.github.lukmnh.toolbox.productivity.rename.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.rename.domain.FileEntry;
import io.github.lukmnh.toolbox.productivity.rename.domain.FileOrder;
import io.github.lukmnh.toolbox.productivity.rename.domain.NameIssue;
import io.github.lukmnh.toolbox.productivity.rename.domain.PreviewRow;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenamePreview;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameStatus;
import io.github.lukmnh.toolbox.productivity.rename.domain.StoredRename;
import java.io.File;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Kolom kanan: input file/folder + urutan, tabel preview Nama lama → Nama baru, ringkasan, Eksekusi & Undo. */
final class PreviewPane extends VBox {

    private static final double INDEX_COLUMN_WIDTH = 60;
    private static final double NAME_COLUMN_WIDTH = 260;
    private static final double STATUS_COLUMN_WIDTH = 140;
    private static final DateTimeFormatter HISTORY_TIME =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Messages.LOCALE);

    private final RenameViewModel viewModel;
    private final Window owner;
    private final TableView<FileEntry> table = new TableView<>();

    PreviewPane(RenameViewModel viewModel, Window owner) {
        super(ToolLayout.SPACING);
        this.viewModel = viewModel;
        this.owner = owner;
        VBox previewCard = buildPreview();
        VBox.setVgrow(previewCard, Priority.ALWAYS);
        getChildren().addAll(buildInput(), previewCard);
    }

    private VBox buildInput() {
        VBox dropZone = ToolLayout.dropZone(
                Messages.get("productivity.rename.drop"),
                Messages.get("productivity.rename.chooseFiles"),
                this::chooseFiles);
        dropZone.getChildren()
                .add(ToolLayout.button(Messages.get("productivity.rename.chooseFolder"), this::chooseFolder));
        dropZone.disableProperty().bind(viewModel.busyProperty());
        acceptFileDrops(dropZone);

        Label summary = ToolLayout.statText();
        summary.setWrapText(true);
        summary.textProperty()
                .bind(Bindings.createStringBinding(this::batchSummary, viewModel.files(), viewModel.folderProperty()));
        Button clear = ToolLayout.button(Messages.get("productivity.rename.clear"), viewModel::clearFiles);
        clear.disableProperty().bind(Bindings.isEmpty(viewModel.files()).or(viewModel.busyProperty()));
        return ToolLayout.card(
                Messages.get("productivity.rename.input.title"),
                List.of(),
                dropZone,
                ToolLayout.row(summary, ToolLayout.spacer(), clear));
    }

    private VBox buildPreview() {
        configureTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        Label previewError = ToolLayout.inlineMessage(Tone.DANGER);
        previewError.textProperty().bind(viewModel.previewErrorProperty());
        Label extensionWarning = ToolLayout.inlineMessage(Tone.WARNING);
        extensionWarning
                .textProperty()
                .bind(Bindings.when(viewModel.changesExtension())
                        .then(Messages.get("productivity.rename.extension.warning"))
                        .otherwise(""));
        return ToolLayout.card(
                Messages.get("productivity.rename.preview.title"),
                List.of(),
                buildOrderRow(),
                table,
                buildStatusRow(),
                previewError,
                extensionWarning,
                buildProgress(),
                buildActions());
    }

    private HBox buildOrderRow() {
        HBox order = ToolLayout.segmented(viewModel.orderProperty(), List.of(FileOrder.values()), RenameTexts::order);
        Button up = ToolLayout.button(Messages.get("productivity.rename.moveUp"), () -> moveSelected(-1));
        Button down = ToolLayout.button(Messages.get("productivity.rename.moveDown"), () -> moveSelected(1));
        for (Button button : List.of(up, down)) {
            button.disableProperty()
                    .bind(table.getSelectionModel()
                            .selectedItemProperty()
                            .isNull()
                            .or(viewModel.busyProperty()));
        }
        return ToolLayout.row(
                ToolLayout.labeled(Messages.get("productivity.rename.order"), order),
                ToolLayout.spacer(),
                ToolLayout.hint(Messages.get("productivity.rename.order.hint")),
                up,
                down);
    }

    private void configureTable() {
        table.setItems(viewModel.files());
        table.setPlaceholder(ToolLayout.hint(Messages.get("productivity.rename.preview.empty")));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns()
                .setAll(List.of(
                        indexColumn(),
                        column("productivity.rename.column.old", NAME_COLUMN_WIDTH, FileEntry::name),
                        column("productivity.rename.column.new", NAME_COLUMN_WIDTH, this::newName),
                        column("productivity.rename.column.size", INDEX_COLUMN_WIDTH * 2, this::size),
                        statusColumn()));
        table.setRowFactory(view -> draggableRow());
        acceptFileDrops(table);
        // Baris = daftar file (langsung ikut urutan); nama baru diambil dari preview terakhir
        viewModel.rowsByNameProperty().addListener((observable, previous, rows) -> table.refresh());
    }

    private HBox buildStatusRow() {
        Map<RenameStatus, Label> badges = new EnumMap<>(RenameStatus.class);
        HBox row = ToolLayout.row();
        for (RenameStatus status : RenameStatus.values()) {
            Label badge = ToolLayout.badge("", RenameTexts.tone(status));
            badges.put(status, badge);
            row.getChildren().add(badge);
        }
        Label pending = ToolLayout.statText();
        row.getChildren().addAll(ToolLayout.spacer(), pending);
        Runnable update = () -> {
            RenamePreview preview = viewModel.previewProperty().get();
            badges.forEach((status, badge) -> badge.setText(Messages.format(
                    "productivity.rename.status.count",
                    RenameTexts.status(status),
                    preview == null ? 0 : preview.count(status))));
            boolean calculating = preview == null && !viewModel.files().isEmpty();
            pending.setText(calculating ? Messages.get("productivity.rename.preview.calculating") : "");
        };
        viewModel.previewProperty().addListener(observable -> update.run());
        update.run();
        return row;
    }

    private ProgressBar buildProgress() {
        ProgressBar progress = new ProgressBar();
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.visibleProperty().bind(viewModel.busyProperty());
        ToolLayout.collapseWhenHidden(progress);
        viewModel
                .currentTaskProperty()
                .addListener((observable, previous, task) ->
                        progress.progressProperty().bind(task.progressProperty()));
        return progress;
    }

    private HBox buildActions() {
        Label historyWarning = ToolLayout.inlineMessage(Tone.WARNING);
        historyWarning.textProperty().bind(viewModel.historyWarningProperty());
        Label lastRename = ToolLayout.hint("");
        lastRename
                .textProperty()
                .bind(Bindings.createStringBinding(this::lastRenameText, viewModel.lastRenameProperty()));
        Button undo = ToolLayout.button(Messages.get("productivity.rename.undo"), viewModel::undo);
        undo.disableProperty().bind(viewModel.canUndo().not());
        Button execute = ToolLayout.accentButton(Messages.get("productivity.rename.execute"), viewModel::execute);
        execute.disableProperty().bind(viewModel.canExecute().not());
        VBox history = new VBox(ToolLayout.GAP / 2, lastRename, historyWarning);
        HBox.setHgrow(history, Priority.ALWAYS);
        return ToolLayout.row(history, undo, execute);
    }

    private TableColumn<FileEntry, String> indexColumn() {
        TableColumn<FileEntry, String> column = new TableColumn<>(Messages.get("productivity.rename.column.index"));
        column.setPrefWidth(INDEX_COLUMN_WIDTH);
        column.setSortable(false);
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : Integer.toString(getIndex() + 1));
            }
        });
        return column;
    }

    private TableColumn<FileEntry, String> statusColumn() {
        TableColumn<FileEntry, String> column =
                column("productivity.rename.column.status", STATUS_COLUMN_WIDTH, file -> "");
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                FileEntry file =
                        empty || getTableRow() == null ? null : getTableRow().getItem();
                PreviewRow row = file == null ? null : viewModel.rowFor(file).orElse(null);
                setGraphic(row == null ? null : statusBadge(row));
                boolean hasIssue = row != null && row.issue() != NameIssue.NONE;
                setTooltip(hasIssue ? new Tooltip(RenameTexts.issue(row.issue())) : null);
            }
        });
        return column;
    }

    /** Alasan KONFLIK / TIDAK VALID langsung terbaca di badge, detailnya di tooltip. */
    private static Label statusBadge(PreviewRow row) {
        String text = row.issue() == NameIssue.NONE
                ? RenameTexts.status(row.status())
                : Messages.format(
                        "productivity.rename.status.withIssue",
                        RenameTexts.status(row.status()),
                        RenameTexts.issue(row.issue()));
        return ToolLayout.badge(text, RenameTexts.tone(row.status()));
    }

    /** Kolom tabel tidak bisa di-sort: urutan baris = urutan penomoran, diatur lewat pilihan Urutan. */
    private static TableColumn<FileEntry, String> column(
            String titleKey, double width, Function<FileEntry, String> value) {
        TableColumn<FileEntry, String> column = new TableColumn<>(Messages.get(titleKey));
        column.setPrefWidth(width);
        column.setSortable(false);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private String newName(FileEntry file) {
        return viewModel.rowFor(file).map(PreviewRow::newName).orElse("");
    }

    private String size(FileEntry file) {
        return file.symbolicLink()
                ? Messages.get("productivity.rename.symlink")
                : ByteSizeFormat.format(file.sizeBytes());
    }

    /** Drag baris untuk urutan manual. Isi drag = indeks baris asal di tabel ini. */
    private TableRow<FileEntry> draggableRow() {
        TableRow<FileEntry> row = new TableRow<>();
        row.setOnDragDetected(event -> {
            if (row.isEmpty() || viewModel.busyProperty().get()) {
                return;
            }
            Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(Integer.toString(row.getIndex()));
            dragboard.setContent(content);
            event.consume();
        });
        row.setOnDragOver(event -> {
            if (isRowDrag(event)) {
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            }
        });
        row.setOnDragDropped(event -> {
            if (!isRowDrag(event)) {
                return;
            }
            int from = Integer.parseInt(event.getDragboard().getString());
            int to = row.isEmpty() ? table.getItems().size() - 1 : row.getIndex();
            viewModel.moveFile(from, to);
            table.getSelectionModel().clearAndSelect(to);
            event.setDropCompleted(true);
            event.consume();
        });
        return row;
    }

    /** Hanya drag yang berasal dari baris tabel ini; drag file dari file manager tidak punya gesture source. */
    private boolean isRowDrag(DragEvent event) {
        return event.getGestureSource() instanceof TableRow<?> source
                && source.getTableView() == table
                && event.getDragboard().hasString();
    }

    /**
     * {@code ToolLayout.acceptDrops} memfilter ekstensi, sedangkan rename menerima file apa pun dan juga folder,
     * jadi handler drop dipasang langsung.
     */
    private void acceptFileDrops(Node target) {
        target.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (event.getGestureSource() == null && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                event.consume();
            }
        });
        target.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            boolean external =
                    event.getGestureSource() == null && event.getDragboard().hasFiles();
            if (!external || viewModel.busyProperty().get()) {
                return;
            }
            viewModel.load(
                    event.getDragboard().getFiles().stream().map(File::toPath).toList());
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private void moveSelected(int delta) {
        int index = table.getSelectionModel().getSelectedIndex();
        int target = index + delta;
        if (index < 0 || target < 0 || target >= table.getItems().size()) {
            return;
        }
        viewModel.moveFile(index, target);
        table.getSelectionModel().clearAndSelect(target);
        table.scrollTo(Math.max(0, target - 1));
    }

    private void chooseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("productivity.rename.chooseFiles.title"));
        List<File> chosen = chooser.showOpenMultipleDialog(owner);
        if (chosen != null && !chosen.isEmpty()) {
            viewModel.load(chosen.stream().map(File::toPath).toList());
        }
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(Messages.get("productivity.rename.chooseFolder.title"));
        File chosen = chooser.showDialog(owner);
        if (chosen != null) {
            viewModel.load(List.of(chosen.toPath()));
        }
    }

    private String batchSummary() {
        Path folder = viewModel.folderProperty().get();
        if (folder == null || viewModel.files().isEmpty()) {
            return Messages.get("productivity.rename.files.none");
        }
        return Messages.format(
                "productivity.rename.files.selected", viewModel.files().size(), String.valueOf(folder.getFileName()));
    }

    private String lastRenameText() {
        StoredRename last = viewModel.lastRenameProperty().get();
        if (last == null) {
            return Messages.get("productivity.rename.undo.none");
        }
        return Messages.format(
                "productivity.rename.undo.last",
                last.log().pairs().size(),
                String.valueOf(last.log().folder().getFileName()),
                HISTORY_TIME.format(last.log().createdAt().atZone(ZoneId.systemDefault())));
    }
}
