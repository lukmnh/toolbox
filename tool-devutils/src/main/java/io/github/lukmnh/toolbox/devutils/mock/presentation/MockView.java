package io.github.lukmnh.toolbox.devutils.mock.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockFormat;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockLocale;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockSchemaValidator;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockType;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Layout Mock: editor kolom, setting (locale, baris, seed, format, tabel), lalu preview + download. */
final class MockView {

    private static final double NAME_WIDTH = 170;
    private static final double TYPE_WIDTH = 150;
    private static final double OPTIONS_WIDTH = 260;
    private static final double ROWS_WIDTH = 120;
    private static final double SEED_WIDTH = 140;
    private static final double TABLE_WIDTH = 160;
    private static final double OUTPUT_HEIGHT = 320;
    private static final int ROWS_STEP = 100;

    private final MockViewModel viewModel;
    private final VBox columnRows = new VBox(ToolLayout.GAP);

    private MockView(MockViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static Node create(MockViewModel viewModel) {
        return new MockView(viewModel).build();
    }

    private Node build() {
        TaskStatusBar status = new TaskStatusBar();
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));
        Label error = ToolLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.errorProperty());

        VBox page = ToolLayout.page();
        page.getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.DEV_UTILS,
                                Messages.get("devutils.mock.name"),
                                Messages.get("devutils.mock.hint")),
                        buildColumns(),
                        buildSettings(),
                        error,
                        buildOutput(),
                        status);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    // ---------- Kolom ----------

    private VBox buildColumns() {
        Button add = ToolLayout.button(Messages.get("devutils.mock.column.add"), viewModel::addColumn);
        add.disableProperty()
                .bind(Bindings.size(viewModel.columns()).greaterThanOrEqualTo(MockSchemaValidator.MAX_COLUMNS));
        viewModel.columns().addListener((ListChangeListener<ColumnDraft>) change -> renderColumns());
        renderColumns();
        return ToolLayout.card(
                Messages.get("devutils.mock.columns"),
                List.of(add),
                columnRows,
                ToolLayout.hint(Messages.get("devutils.mock.columns.hint")));
    }

    private void renderColumns() {
        columnRows
                .getChildren()
                .setAll(viewModel.columns().stream().map(this::columnRow).toList());
    }

    private HBox columnRow(ColumnDraft draft) {
        TextField name = ToolLayout.mono(new TextField());
        name.setPrefWidth(NAME_WIDTH);
        name.setPromptText(Messages.get("devutils.mock.column.name"));
        name.textProperty().bindBidirectional(draft.nameProperty());

        ChoiceBox<MockType> type = new ChoiceBox<>();
        type.getItems().setAll(MockType.values());
        type.setConverter(ToolLayout.labels(MockTexts::type));
        type.setPrefWidth(TYPE_WIDTH);
        type.valueProperty().bindBidirectional(draft.typeProperty());

        TextField options = ToolLayout.mono(new TextField());
        options.setPrefWidth(OPTIONS_WIDTH);
        HBox.setHgrow(options, Priority.ALWAYS);
        options.textProperty().bindBidirectional(draft.optionsProperty());
        options.promptTextProperty().bind(draft.typeProperty().map(MockTexts::options));
        options.disableProperty().bind(draft.typeProperty().map(value -> !value.hasOptions()));

        Button remove =
                ToolLayout.button(Messages.get("devutils.mock.column.remove"), () -> viewModel.removeColumn(draft));
        remove.disableProperty().bind(Bindings.size(viewModel.columns()).lessThanOrEqualTo(1));
        return ToolLayout.row(name, type, options, remove);
    }

    // ---------- Setting ----------

    private VBox buildSettings() {
        Spinner<Integer> rows = new Spinner<>(
                1, MockSchemaValidator.MAX_ROWS, viewModel.rowsProperty().get(), ROWS_STEP);
        rows.setEditable(true);
        rows.setPrefWidth(ROWS_WIDTH);
        rows.getValueFactory().valueProperty().bindBidirectional(viewModel.rowsProperty());

        TextField seed = ToolLayout.mono(new TextField());
        seed.setPrefWidth(SEED_WIDTH);
        seed.textProperty().bindBidirectional(viewModel.seedProperty());
        Button newSeed = ToolLayout.button(Messages.get("devutils.mock.seed.new"), viewModel::newSeed);

        TextField table = ToolLayout.mono(new TextField());
        table.setPrefWidth(TABLE_WIDTH);
        table.textProperty().bindBidirectional(viewModel.tableProperty());
        VBox tableField = ToolLayout.labeled(Messages.get("devutils.mock.table"), table);
        tableField.visibleProperty().bind(viewModel.formatProperty().isEqualTo(MockFormat.SQL));
        ToolLayout.collapseWhenHidden(tableField);

        HBox row = ToolLayout.row(
                ToolLayout.labeled(
                        Messages.get("devutils.mock.locale"),
                        ToolLayout.segmented(
                                viewModel.localeProperty(), List.of(MockLocale.values()), MockTexts::locale)),
                ToolLayout.labeled(Messages.get("devutils.mock.rows"), rows),
                ToolLayout.labeled(Messages.get("devutils.mock.seed"), ToolLayout.row(seed, newSeed)),
                ToolLayout.labeled(
                        Messages.get("devutils.mock.format"),
                        ToolLayout.segmented(viewModel.formatProperty(), List.of(MockFormat.values()), Enum::name)),
                tableField);
        row.setSpacing(ToolLayout.SPACING * 2);
        return ToolLayout.card(
                Messages.get("devutils.mock.settings"),
                List.of(),
                row,
                ToolLayout.hint(Messages.get("devutils.mock.seed.hint")));
    }

    // ---------- Output ----------

    private VBox buildOutput() {
        TextArea output = ToolLayout.codeArea(false);
        output.setWrapText(false);
        output.setPrefHeight(OUTPUT_HEIGHT);
        output.textProperty().bind(viewModel.previewProperty());

        Label info = ToolLayout.hint("");
        info.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> Messages.format(
                                "devutils.mock.previewInfo",
                                viewModel.previewRows(),
                                viewModel.rowsProperty().get()),
                        viewModel.rowsProperty()));

        Button copy = ToolLayout.button(Messages.get("devutils.mock.copyPreview"), viewModel::copy);
        HBox downloads = ToolLayout.downloadButtons(viewModel::download, viewModel::saveAs);
        downloads.disableProperty().bind(viewModel.busyProperty());
        return ToolLayout.card(Messages.get("devutils.mock.output"), List.of(copy), output, info, downloads);
    }
}
