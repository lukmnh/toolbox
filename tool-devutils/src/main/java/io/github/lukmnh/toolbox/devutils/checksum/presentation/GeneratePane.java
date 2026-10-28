package io.github.lukmnh.toolbox.devutils.checksum.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.devutils.checksum.domain.ChecksumAlgorithm;
import io.github.lukmnh.toolbox.devutils.checksum.domain.FileChecksum;
import io.github.lukmnh.toolbox.devutils.shared.presentation.ClipboardActions;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevLayout;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Tab Generate: pilih/drop banyak file, pilih algoritma, lalu tabel hasil + download {@code SHA256SUMS}. */
final class GeneratePane extends VBox {

    private static final double CHECKSUM_COLUMN_WIDTH = 460;
    private static final double NAME_COLUMN_WIDTH = 220;
    private static final double SMALL_COLUMN_WIDTH = 100;
    private static final int DOUBLE_CLICK = 2;

    private final GenerateViewModel viewModel;
    private final Window owner;
    private final Notifier notifier;

    GeneratePane(GenerateViewModel viewModel, Window owner, Notifier notifier) {
        super(DevLayout.SPACING);
        this.viewModel = viewModel;
        this.owner = owner;
        this.notifier = notifier;
        setPadding(new Insets(DevLayout.SPACING, 0, 0, 0));
        VBox results = buildResults();
        VBox.setVgrow(results, Priority.ALWAYS);
        getChildren().addAll(buildInput(), results);
    }

    private VBox buildInput() {
        VBox dropZone = DevLayout.dropZone(
                Messages.get("devutils.checksum.generate.drop"),
                Messages.get("devutils.checksum.chooseFiles"),
                this::chooseFiles);
        DevLayout.acceptDrops(dropZone, Set.of(), viewModel::addFiles, this::warnRejected);

        Label selected = DevLayout.statText();
        selected.setWrapText(true);
        selected.textProperty().bind(Bindings.createStringBinding(viewModel::filesSummary, viewModel.files()));

        Button clear = DevLayout.button(Messages.get("devutils.checksum.files.clear"), viewModel::clearFiles);
        clear.disableProperty().bind(Bindings.isEmpty(viewModel.files()));
        Button run = DevLayout.accentButton(Messages.get("devutils.checksum.generate.run"), viewModel::run);
        run.disableProperty().bind(viewModel.canRun().not());

        return DevLayout.card(
                Messages.get("devutils.checksum.generate.input"),
                List.of(),
                dropZone,
                selected,
                DevLayout.row(
                        DevLayout.labeled(
                                Messages.get("devutils.checksum.algorithm"),
                                DevLayout.segmented(
                                        viewModel.algorithmProperty(),
                                        List.of(ChecksumAlgorithm.values()),
                                        ChecksumAlgorithm::label)),
                        DevLayout.spacer(),
                        clear,
                        run));
    }

    private VBox buildResults() {
        TableView<FileChecksum> table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        Label summary = DevLayout.statText();
        summary.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> viewModel.results().isEmpty() ? "" : viewModel.resultSummary(), viewModel.results()));

        Button copy = ClipboardActions.copyButton(viewModel::sumsText, notifier);
        copy.setTooltip(new Tooltip(Messages.get("devutils.checksum.copySums.tooltip")));
        Button download = DevLayout.accentButton(Messages.get("download.button"), viewModel::download);
        Button saveAs = DevLayout.button(Messages.get("download.saveAs.button"), viewModel::saveAs);
        for (Button button : List.of(copy, download, saveAs)) {
            button.disableProperty().bind(viewModel.hasResults().not());
        }

        VBox card = DevLayout.card(
                Messages.get("devutils.checksum.generate.result"),
                List.of(copy, saveAs, download),
                table,
                DevLayout.row(
                        summary, DevLayout.spacer(), DevLayout.hint(Messages.get("devutils.checksum.copyRow.hint"))));
        return card;
    }

    private TableView<FileChecksum> buildTable() {
        TableView<FileChecksum> table = new TableView<>(viewModel.results());
        table.setPlaceholder(DevLayout.hint(Messages.get("devutils.checksum.generate.empty")));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns()
                .setAll(List.of(
                        column("devutils.checksum.column.file", NAME_COLUMN_WIDTH, FileChecksum::fileName, false),
                        column(
                                "devutils.checksum.column.size",
                                SMALL_COLUMN_WIDTH,
                                row -> ByteSizeFormat.format(row.sizeBytes()),
                                false),
                        column(
                                "devutils.checksum.column.duration",
                                SMALL_COLUMN_WIDTH,
                                row -> Messages.format(
                                        "devutils.checksum.seconds", GenerateViewModel.seconds(row.duration())),
                                false),
                        column(
                                "devutils.checksum.column.checksum",
                                CHECKSUM_COLUMN_WIDTH,
                                FileChecksum::checksum,
                                true)));
        table.setRowFactory(view -> copyOnDoubleClick());
        return table;
    }

    /** Double-click baris → copy checksum baris itu. */
    private TableRow<FileChecksum> copyOnDoubleClick() {
        TableRow<FileChecksum> row = new TableRow<>();
        row.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == DOUBLE_CLICK && !row.isEmpty()) {
                ClipboardActions.copy(row.getItem().checksum(), notifier);
            }
        });
        return row;
    }

    static <T> TableColumn<T, String> column(
            String titleKey, double width, Function<T, String> value, boolean monospace) {
        TableColumn<T, String> column = new TableColumn<>(Messages.get(titleKey));
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        if (monospace) {
            column.setCellFactory(ignored -> monospaceCell());
        }
        return column;
    }

    private static <T> TableCell<T, String> monospaceCell() {
        TableCell<T, String> cell = new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        };
        cell.getStyleClass().add("mono");
        return cell;
    }

    private void warnRejected(String names) {
        notifier.warning(Messages.format("devutils.drop.rejected", names));
    }

    private void chooseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("devutils.checksum.chooseFiles.title"));
        List<File> chosen = chooser.showOpenMultipleDialog(owner);
        if (chosen != null && !chosen.isEmpty()) {
            viewModel.addFiles(chosen.stream().map(File::toPath).toList());
        }
    }
}
