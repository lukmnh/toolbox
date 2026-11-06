package io.github.lukmnh.toolbox.devutils.sql.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataFiles;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataTable;
import io.github.lukmnh.toolbox.devutils.sql.domain.DataValue;
import io.github.lukmnh.toolbox.devutils.sql.domain.DmlOperation;
import io.github.lukmnh.toolbox.devutils.sql.domain.SyntaxTarget;
import java.io.File;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Mode Generator: card Data (upload + preview tabel), Options (database, operasi, tabel, key), dan Result. */
final class GeneratorView {

    /** Preview tabel hanya beberapa baris pertama; data lengkap tetap dipakai saat generate. */
    static final int PREVIEW_ROWS = 50;

    private static final double PREVIEW_HEIGHT = 220;
    private static final double OUTPUT_HEIGHT = 360;
    private static final double TABLE_FIELD_WIDTH = 220;
    private static final double KEY_CHOICE_WIDTH = 180;
    private static final String NULL_DISPLAY = "NULL";

    private final GeneratorViewModel viewModel;
    private final ToolContext context;

    private GeneratorView(GeneratorViewModel viewModel, ToolContext context) {
        this.viewModel = viewModel;
        this.context = context;
    }

    static Node create(GeneratorViewModel viewModel, ToolContext context) {
        return new GeneratorView(viewModel, context).build();
    }

    private Node build() {
        TaskStatusBar status = new TaskStatusBar();
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));
        VBox root = new VBox(ToolLayout.SPACING, buildData(), buildOptions(), status, buildResult());
        root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
        return root;
    }

    /** Ctrl+Enter: Generate. */
    private void handleShortcut(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER
                && event.isShortcutDown()
                && viewModel.canGenerate().get()) {
            viewModel.generate();
            event.consume();
        }
    }

    private VBox buildData() {
        Button upload = ToolLayout.button(Messages.get("devutils.sql.upload"), this::chooseFile);
        Button clear = ToolLayout.button(Messages.get("devutils.sql.clear"), viewModel::clear);
        clear.disableProperty().bind(viewModel.dataProperty().isNull());

        VBox dropZone = ToolLayout.dropZone(
                Messages.get("devutils.sql.generator.drop"), Messages.get("devutils.sql.upload"), this::chooseFile);
        dropZone.visibleProperty().bind(viewModel.dataProperty().isNull());
        ToolLayout.collapseWhenHidden(dropZone);

        Label info = ToolLayout.statText();
        info.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> summary(viewModel.dataProperty().get()), viewModel.dataProperty()));

        TableView<List<DataValue>> table = new TableView<>();
        table.setPrefHeight(PREVIEW_HEIGHT);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        viewModel.dataProperty().addListener((observable, previous, current) -> showPreview(table, current));
        VBox loaded = new VBox(ToolLayout.GAP, info, table);
        loaded.visibleProperty().bind(viewModel.dataProperty().isNotNull());
        ToolLayout.collapseWhenHidden(loaded);

        VBox card = ToolLayout.card(
                Messages.get("devutils.sql.generator.data"),
                List.of(upload, clear),
                dropZone,
                loaded,
                ToolLayout.hint(Messages.get("devutils.sql.generator.formats")));
        ToolLayout.acceptDrops(
                card,
                DataFiles.EXTENSIONS,
                files -> viewModel.loadFile(files.getFirst()),
                names -> context.notifier().warning(Messages.format("devutils.drop.rejected", names)));
        return card;
    }

    private static String summary(DataTable data) {
        if (data == null) {
            return "";
        }
        return Messages.format(
                "devutils.sql.generator.summary",
                data.sourceName(),
                data.rows().size(),
                data.columns().size(),
                Math.min(PREVIEW_ROWS, data.rows().size()));
    }

    /** Kolom TableView dibangun ulang tiap data baru karena jumlah dan nama kolom berubah. */
    private static void showPreview(TableView<List<DataValue>> table, DataTable data) {
        table.getColumns().clear();
        if (data == null) {
            table.getItems().clear();
            return;
        }
        for (int index = 0; index < data.columns().size(); index++) {
            int column = index;
            TableColumn<List<DataValue>, String> tableColumn =
                    new TableColumn<>(data.columns().get(column));
            tableColumn.setSortable(false);
            tableColumn.setCellValueFactory(cell -> {
                DataValue value = cell.getValue().get(column);
                return new ReadOnlyStringWrapper(value.isNull() ? NULL_DISPLAY : value.text());
            });
            table.getColumns().add(tableColumn);
        }
        table.getItems()
                .setAll(data.rows()
                        .subList(0, Math.min(PREVIEW_ROWS, data.rows().size())));
    }

    private VBox buildOptions() {
        TextField table = ToolLayout.codeField();
        table.setPrefWidth(TABLE_FIELD_WIDTH);
        table.setPromptText(Messages.get("devutils.sql.generator.table.prompt"));
        table.textProperty().bindBidirectional(viewModel.tableProperty());

        ChoiceBox<String> key = new ChoiceBox<>();
        key.setPrefWidth(KEY_CHOICE_WIDTH);
        viewModel
                .dataProperty()
                .addListener((observable, previous, current) ->
                        key.getItems().setAll(current == null ? List.of() : current.columns()));
        key.valueProperty().bindBidirectional(viewModel.keyColumnProperty());
        key.disableProperty().bind(viewModel.operationProperty().isEqualTo(DmlOperation.INSERT));

        CheckBox allText = new CheckBox(Messages.get("devutils.sql.generator.allText"));
        allText.selectedProperty().bindBidirectional(viewModel.allTextProperty());
        allText.setTooltip(new Tooltip(Messages.get("devutils.sql.generator.allText.tooltip")));

        Button generate = ToolLayout.accentButton(Messages.get("devutils.sql.generator.action"), viewModel::generate);
        generate.setTooltip(new Tooltip(Messages.get("devutils.sql.format.tooltip")));
        generate.disableProperty().bind(viewModel.canGenerate().not());

        HBox choiceRow = ToolLayout.row(
                ToolLayout.labeled(
                        Messages.get("devutils.sql.generator.target"),
                        ToolLayout.segmented(
                                viewModel.targetProperty(), List.of(SyntaxTarget.values()), SyntaxTarget::displayName)),
                ToolLayout.labeled(
                        Messages.get("devutils.sql.generator.operation"),
                        ToolLayout.segmented(
                                viewModel.operationProperty(), List.of(DmlOperation.values()), DmlOperation::name)));
        choiceRow.setSpacing(ToolLayout.SPACING);
        HBox fieldRow = ToolLayout.row(
                ToolLayout.labeled(Messages.get("devutils.sql.generator.table"), table),
                ToolLayout.labeled(Messages.get("devutils.sql.generator.key"), key),
                allText,
                ToolLayout.spacer(),
                generate);
        fieldRow.setSpacing(ToolLayout.SPACING);

        Label problem = ToolLayout.inlineMessage(Tone.WARNING);
        problem.textProperty().bind(viewModel.problem());
        return ToolLayout.card(
                choiceRow, fieldRow, problem, ToolLayout.hint(Messages.get("devutils.sql.generator.rules")));
    }

    private VBox buildResult() {
        Label info = ToolLayout.statText();
        info.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> describe(viewModel.resultProperty().get()), viewModel.resultProperty()));

        TextArea output = ToolLayout.codeArea(false);
        output.setWrapText(false);
        output.setPrefHeight(OUTPUT_HEIGHT);
        output.textProperty().bind(viewModel.previewProperty());

        Button copy = ToolLayout.button(Messages.get("common.copy"), viewModel::copy);
        Button download = ToolLayout.accentButton(Messages.get("download.button"), viewModel::download);
        Button saveAs = ToolLayout.button(Messages.get("download.saveAs.button"), viewModel::saveAs);
        for (Button button : List.of(copy, download, saveAs)) {
            button.disableProperty().bind(viewModel.canExport().not());
        }

        VBox card = ToolLayout.card(
                Messages.get("devutils.sql.result"),
                List.of(),
                info,
                skippedMessage(),
                staleMessage(),
                truncatedMessage(),
                output,
                ToolLayout.row(ToolLayout.spacer(), copy, saveAs, download));
        card.visibleProperty().bind(viewModel.resultProperty().isNotNull());
        ToolLayout.collapseWhenHidden(card);
        return card;
    }

    private static String describe(GeneratorViewModel.Run run) {
        if (run == null) {
            return "";
        }
        return Messages.format(
                "devutils.sql.generator.result.info",
                run.target().displayName(),
                run.operation().name(),
                run.syntax().rows(),
                ByteSizeFormat.format(run.outputBytes()));
    }

    private Label skippedMessage() {
        Label message = ToolLayout.inlineMessage(Tone.WARNING);
        message.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            GeneratorViewModel.Run run =
                                    viewModel.resultProperty().get();
                            int skipped = run == null ? 0 : run.syntax().skippedRows();
                            return skipped == 0 ? "" : Messages.format("devutils.sql.generator.skipped", skipped);
                        },
                        viewModel.resultProperty()));
        return message;
    }

    private Label staleMessage() {
        Label message = ToolLayout.inlineMessage(Tone.WARNING);
        message.textProperty()
                .bind(Bindings.when(viewModel.staleProperty())
                        .then(Messages.get("devutils.sql.generator.stale"))
                        .otherwise(""));
        return message;
    }

    private Label truncatedMessage() {
        Label message = ToolLayout.inlineMessage(Tone.INFO);
        message.textProperty()
                .bind(Bindings.when(viewModel.previewTruncatedProperty())
                        .then(Messages.format("devutils.sql.previewTruncated", SqlViewModel.EDITOR_MAX_CHARS))
                        .otherwise(""));
        return message;
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("devutils.sql.generator.chooser.title"));
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(
                        Messages.get("devutils.sql.generator.chooser.filter"),
                        DataFiles.EXTENSIONS.stream()
                                .map(extension -> "*." + extension)
                                .toList()));
        File chosen = chooser.showOpenDialog(context.owner());
        if (chosen != null) {
            viewModel.loadFile(chosen.toPath());
        }
    }
}
