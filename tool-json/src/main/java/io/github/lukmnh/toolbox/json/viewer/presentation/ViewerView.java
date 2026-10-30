package io.github.lukmnh.toolbox.json.viewer.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.json.shared.presentation.JsonErrorMessages;
import io.github.lukmnh.toolbox.json.viewer.domain.ExportFormat;
import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

/** Layout JSON Viewer: toolbar, filter, tabel/tree + detail, export, dan status bar. */
public final class ViewerView extends VBox {

    private static final double TABLE_DIVIDER = 0.65;
    private static final double QUERY_FIELD_WIDTH = 420;
    private static final double JUMP_FIELD_WIDTH = 110;

    private final ViewerViewModel viewModel;
    private final Window owner;
    private final RecordTable table;

    public ViewerView(ViewerViewModel viewModel, Window owner, Consumer<String> warn) {
        super(ToolLayout.SPACING);
        this.viewModel = viewModel;
        this.owner = owner;
        this.table = new RecordTable(viewModel);
        getStyleClass().add("tool-view");
        setPadding(new Insets(ToolLayout.SPACING * 2));
        ToolLayout.acceptDrops(
                this,
                ViewerViewModel.EXTENSIONS,
                files -> viewModel.openFile(files.getFirst()),
                names -> warn.accept(Messages.format("json.viewer.drop.rejected", names)));

        VBox dropZone = ToolLayout.dropZone(
                Messages.get("json.viewer.drop"), Messages.get("json.viewer.open"), this::chooseFile);
        dropZone.visibleProperty().bind(viewModel.sessionProperty().isNull());
        ToolLayout.collapseWhenHidden(dropZone);

        TaskStatusBar status = new TaskStatusBar();
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));

        StackPane content = new StackPane(buildTableArea(), buildTreeArea());
        VBox.setVgrow(content, Priority.ALWAYS);

        getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.JSON, Messages.get("json.viewer.name"), Messages.get("json.viewer.hint")),
                        buildToolbar(),
                        dropZone,
                        buildFilterBar(),
                        buildQueryError(),
                        status,
                        content,
                        buildExportBar(),
                        buildStatusLabel());
    }

    private HBox buildToolbar() {
        Button open = new Button(Messages.get("json.viewer.open"));
        open.setOnAction(event -> chooseFile());
        MenuButton recent = new MenuButton(Messages.get("json.viewer.recent"));
        recent.disableProperty().bind(Bindings.isEmpty(viewModel.recentFiles()));
        viewModel.recentFiles().addListener((ListChangeListener<Path>) change -> recent.getItems()
                .setAll(viewModel.recentFiles().stream().map(this::recentItem).toList()));
        Label info = new Label();
        info.getStyleClass().add("file-info");
        info.textProperty().bind(viewModel.fileSummary());
        HBox row = new HBox(ToolLayout.SPACING, open, recent, info);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private MenuItem recentItem(Path file) {
        MenuItem item = new MenuItem(Messages.format("json.viewer.recent.item", file.getFileName(), file.getParent()));
        item.setOnAction(event -> viewModel.openFile(file));
        return item;
    }

    private HBox buildFilterBar() {
        TextField search = new TextField();
        search.setPromptText(Messages.get("json.viewer.search.prompt"));
        search.textProperty().bindBidirectional(viewModel.quickSearchProperty());
        search.setOnAction(event -> viewModel.applyFilter());

        TextField query = ToolLayout.mono(new TextField());
        query.setPromptText(Messages.get("json.viewer.query.prompt"));
        query.setPrefWidth(QUERY_FIELD_WIDTH);
        query.textProperty().bindBidirectional(viewModel.queryProperty());
        query.setOnAction(event -> viewModel.applyFilter());
        HBox.setHgrow(query, Priority.ALWAYS);

        Button apply = new Button(Messages.get("json.viewer.filter.apply"));
        apply.getStyleClass().add("accent");
        apply.setOnAction(event -> viewModel.applyFilter());
        Button reset = new Button(Messages.get("json.viewer.filter.reset"));
        reset.setOnAction(event -> viewModel.resetFilter());

        TextField jump = new TextField();
        jump.setPromptText(Messages.get("json.viewer.jump.prompt"));
        jump.setPrefWidth(JUMP_FIELD_WIDTH);
        Button go = new Button(Messages.get("json.viewer.jump"));
        Runnable jumpAction = () -> viewModel.positionOf(jump.getText()).ifPresent(table::reveal);
        go.setOnAction(event -> jumpAction.run());
        jump.setOnAction(event -> jumpAction.run());

        // Tombol tidak boleh menyusut saat field query melebar
        for (Button button : new Button[] {apply, reset, go}) {
            button.setMinWidth(Region.USE_PREF_SIZE);
        }
        HBox row = new HBox(8, search, query, apply, reset, ToolLayout.spacer(), jump, go);
        row.setAlignment(Pos.CENTER_LEFT);
        row.disableProperty().bind(viewModel.canQuery().not());
        row.visibleProperty().bind(viewModel.tableMode());
        ToolLayout.collapseWhenHidden(row);
        return row;
    }

    private Label buildQueryError() {
        Label error = ToolLayout.mono(new Label());
        error.getStyleClass().add("text-error");
        error.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            var invalid = viewModel.queryErrorProperty().get();
                            if (invalid == null) {
                                return "";
                            }
                            return JsonErrorMessages.header(invalid) + ": " + JsonErrorMessages.detail(invalid);
                        },
                        viewModel.queryErrorProperty()));
        error.visibleProperty().bind(viewModel.queryErrorProperty().isNotNull());
        ToolLayout.collapseWhenHidden(error);
        return error;
    }

    private SplitPane buildTableArea() {
        SplitPane split = new SplitPane(table, new RecordDetailPane(viewModel));
        split.setDividerPositions(TABLE_DIVIDER);
        split.visibleProperty().bind(viewModel.tableMode());
        return split;
    }

    private JsonTreeView buildTreeArea() {
        JsonTreeView tree = new JsonTreeView(viewModel::loadChildren);
        tree.visibleProperty().bind(viewModel.treeMode());
        viewModel.sessionProperty().addListener((observable, previous, session) -> {
            tree.clear();
            if (session != null && !session.format().isTable()) {
                viewModel.loadRootNode(tree::show);
            }
        });
        return tree;
    }

    private HBox buildExportBar() {
        ChoiceBox<ExportFormat> format = new ChoiceBox<>();
        format.getItems().setAll(ExportFormat.values());
        format.setConverter(new StringConverter<>() {
            @Override
            public String toString(ExportFormat value) {
                return value == null ? "" : viewModel.exportLabel(value);
            }

            @Override
            public ExportFormat fromString(String text) {
                throw new UnsupportedOperationException("ChoiceBox tidak bisa diketik");
            }
        });
        format.valueProperty().bindBidirectional(viewModel.exportFormatProperty());

        Button download = new Button(Messages.get("download.button"));
        download.setOnAction(event -> viewModel.download());
        Button saveAs = new Button(Messages.get("download.saveAs.button"));
        saveAs.setOnAction(event -> viewModel.saveAs());

        Label hint = new Label(Messages.get("json.viewer.export.hint"));
        hint.getStyleClass().add("field-label");
        HBox row = new HBox(
                ToolLayout.SPACING,
                hint,
                ToolLayout.spacer(),
                new Label(Messages.get("json.viewer.export.format")),
                format,
                ToolLayout.downloadButtons(download, saveAs));
        row.setAlignment(Pos.CENTER_LEFT);
        row.disableProperty().bind(viewModel.canQuery().not());
        row.visibleProperty().bind(viewModel.tableMode());
        ToolLayout.collapseWhenHidden(row);
        return row;
    }

    private Label buildStatusLabel() {
        Label status = new Label();
        status.getStyleClass().add("field-label");
        status.textProperty().bind(viewModel.statusText());
        return status;
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("json.viewer.chooser.title"));
        chooser.getExtensionFilters()
                .addAll(new FileChooser.ExtensionFilter(
                        Messages.get("json.chooser.filter.all"),
                        "*.json",
                        "*.JSON",
                        "*.jsonl",
                        "*.JSONL",
                        "*.ndjson",
                        "*.NDJSON"));
        File chosen = chooser.showOpenDialog(owner);
        if (chosen != null) {
            viewModel.openFile(chosen.toPath());
        }
    }
}
