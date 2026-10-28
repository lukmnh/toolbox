package io.github.lukmnh.toolbox.devutils.encoder.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.devutils.encoder.domain.ParsedUrl;
import io.github.lukmnh.toolbox.devutils.encoder.domain.SpaceEncoding;
import io.github.lukmnh.toolbox.devutils.shared.presentation.ClipboardActions;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevLayout;
import io.github.lukmnh.toolbox.devutils.shared.presentation.Tone;
import java.util.List;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Tab URL: kartu encode/decode dan kartu parser URL dengan tabel query parameter. */
final class UrlPane extends VBox {

    private static final int EDITOR_ROWS = 4;
    private static final double TABLE_HEIGHT = 220;
    private static final double KEY_COLUMN_WIDTH = 220;
    private static final double VALUE_COLUMN_WIDTH = 420;

    private final UrlViewModel viewModel;
    private final Notifier notifier;

    UrlPane(UrlViewModel viewModel, Notifier notifier) {
        super(DevLayout.SPACING);
        this.viewModel = viewModel;
        this.notifier = notifier;
        getChildren().addAll(buildCodecCard(), buildParserCard());
    }

    // ---------- Encode / Decode ----------

    private VBox buildCodecCard() {
        HBox spaces =
                DevLayout.segmented(viewModel.spacesProperty(), List.of(SpaceEncoding.values()), UrlPane::spaceLabel);
        VBox spacesBox = DevLayout.labeled(Messages.get("devutils.encoder.url.spaces"), spaces);
        spacesBox.visibleProperty().bind(viewModel.modeProperty().isEqualTo(UrlMode.COMPONENT));
        DevLayout.collapseWhenHidden(spacesBox);
        HBox options = DevLayout.row(
                DevLayout.labeled(
                        Messages.get("devutils.encoder.url.mode"),
                        DevLayout.segmented(viewModel.modeProperty(), List.of(UrlMode.values()), UrlPane::modeLabel)),
                DevLayout.labeled(
                        Messages.get("devutils.encoder.url.direction"),
                        DevLayout.segmented(
                                viewModel.directionProperty(),
                                List.of(Direction.values()),
                                Base64Pane::directionLabel)),
                spacesBox,
                DevLayout.spacer(),
                DevLayout.button(Messages.get("devutils.encoder.swap"), viewModel::swap));
        options.setSpacing(DevLayout.SPACING * 2);

        TextArea input = DevLayout.codeArea(true);
        input.setPrefRowCount(EDITOR_ROWS);
        input.setPromptText(Messages.get("devutils.encoder.url.input.prompt"));
        input.textProperty().bindBidirectional(viewModel.inputProperty());
        TextArea output = DevLayout.codeArea(false);
        output.setPrefRowCount(EDITOR_ROWS);
        output.textProperty().bind(viewModel.outputProperty());
        Label error = DevLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.errorProperty());

        Button copy = DevLayout.button(Messages.get("devutils.copy"), viewModel::copyOutput);
        copy.disableProperty().bind(viewModel.outputProperty().isEmpty());
        return DevLayout.card(
                Messages.get("devutils.encoder.url.codec"),
                List.of(copy, ClipboardActions.clearButton(notifier)),
                options,
                DevLayout.labeled(Messages.get("devutils.encoder.input"), input),
                error,
                DevLayout.labeled(Messages.get("devutils.encoder.output"), output));
    }

    // ---------- Parser URL ----------

    private VBox buildParserCard() {
        TextField url = DevLayout.codeField();
        url.setPromptText(Messages.get("devutils.encoder.url.parse.prompt"));
        url.textProperty().bindBidirectional(viewModel.urlInputProperty());
        Label error = DevLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.parseErrorProperty());

        TableView<ParamRow> table = buildParamTable();
        TextField rebuilt = DevLayout.codeField();
        rebuilt.setEditable(false);
        rebuilt.textProperty().bind(viewModel.rebuiltProperty());
        Button copy = DevLayout.button(Messages.get("devutils.copy"), viewModel::copyRebuilt);
        copy.disableProperty().bind(viewModel.rebuiltProperty().isEmpty());
        HBox.setHgrow(rebuilt, Priority.ALWAYS);

        VBox details = new VBox(
                DevLayout.SPACING,
                buildPartsGrid(),
                DevLayout.labeled(Messages.get("devutils.encoder.url.params"), table),
                buildParamActions(table),
                DevLayout.labeled(Messages.get("devutils.encoder.url.rebuilt"), DevLayout.row(rebuilt, copy)));
        details.visibleProperty().bind(viewModel.parsedProperty().isNotNull());
        DevLayout.collapseWhenHidden(details);

        return DevLayout.card(
                Messages.get("devutils.encoder.url.parser"),
                List.of(),
                DevLayout.hint(Messages.get("devutils.encoder.url.parser.hint")),
                url,
                error,
                details);
    }

    private GridPane buildPartsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(DevLayout.SPACING * 2);
        grid.setVgap(DevLayout.GAP / 2);
        addPart(grid, 0, "devutils.encoder.url.scheme", ParsedUrl::scheme);
        addPart(grid, 1, "devutils.encoder.url.host", ParsedUrl::host);
        addPart(
                grid,
                2,
                "devutils.encoder.url.port",
                parsed -> parsed.hasPort() ? Integer.toString(parsed.port()) : "");
        addPart(grid, 3, "devutils.encoder.url.path", ParsedUrl::path);
        addPart(grid, 4, "devutils.encoder.url.fragment", ParsedUrl::fragment);
        return grid;
    }

    private void addPart(GridPane grid, int row, String labelKey, Function<ParsedUrl, String> part) {
        Label name = new Label(Messages.get(labelKey));
        name.getStyleClass().add("field-label");
        Label value = new Label();
        value.getStyleClass().add("result-value");
        value.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> displayPart(viewModel.parsedProperty().get(), part), viewModel.parsedProperty()));
        grid.addRow(row, name, value);
    }

    private static String displayPart(ParsedUrl parsed, Function<ParsedUrl, String> part) {
        if (parsed == null) {
            return "";
        }
        String value = part.apply(parsed);
        return value.isEmpty() ? Messages.get("devutils.encoder.url.empty") : value;
    }

    private TableView<ParamRow> buildParamTable() {
        TableView<ParamRow> table = new TableView<>(viewModel.params());
        table.setEditable(true);
        table.setPrefHeight(TABLE_HEIGHT);
        table.setPlaceholder(new Label(Messages.get("devutils.encoder.url.params.empty")));
        table.getColumns()
                .setAll(List.of(
                        editableColumn("devutils.encoder.url.key", ParamRow::keyProperty, KEY_COLUMN_WIDTH),
                        editableColumn("devutils.encoder.url.value", ParamRow::valueProperty, VALUE_COLUMN_WIDTH)));
        return table;
    }

    /** Commit edit bawaan TableColumn menulis langsung ke property baris, jadi rebuild URL ikut terpicu. */
    private static TableColumn<ParamRow, String> editableColumn(
            String titleKey, Function<ParamRow, StringProperty> property, double width) {
        TableColumn<ParamRow, String> column = new TableColumn<>(Messages.get(titleKey));
        column.setCellValueFactory(cell -> property.apply(cell.getValue()));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setPrefWidth(width);
        return column;
    }

    private HBox buildParamActions(TableView<ParamRow> table) {
        Button remove = DevLayout.button(
                Messages.get("devutils.encoder.url.params.remove"),
                () -> viewModel.removeParam(table.getSelectionModel().getSelectedIndex()));
        Button up = DevLayout.button(Messages.get("devutils.encoder.url.params.up"), () -> move(table, -1));
        Button down = DevLayout.button(Messages.get("devutils.encoder.url.params.down"), () -> move(table, 1));
        remove.disableProperty()
                .bind(table.getSelectionModel().selectedItemProperty().isNull());
        up.disableProperty()
                .bind(table.getSelectionModel().selectedItemProperty().isNull());
        down.disableProperty()
                .bind(table.getSelectionModel().selectedItemProperty().isNull());
        Button add = DevLayout.button(Messages.get("devutils.encoder.url.params.add"), () -> {
            viewModel.addParam();
            int last = viewModel.params().size() - 1;
            table.getSelectionModel().select(last);
            table.edit(last, table.getColumns().getFirst());
        });
        return DevLayout.row(
                add,
                remove,
                up,
                down,
                DevLayout.button(Messages.get("devutils.encoder.url.params.sort"), viewModel::sortParams));
    }

    private void move(TableView<ParamRow> table, int offset) {
        int index = table.getSelectionModel().getSelectedIndex();
        table.getSelectionModel().select(viewModel.moveParam(index, offset));
    }

    private static String modeLabel(UrlMode mode) {
        return switch (mode) {
            case COMPONENT -> Messages.get("devutils.encoder.url.mode.component");
            case FULL_URL -> Messages.get("devutils.encoder.url.mode.full");
        };
    }

    private static String spaceLabel(SpaceEncoding spaces) {
        return switch (spaces) {
            case PERCENT_20 -> Messages.get("devutils.encoder.url.spaces.percent");
            case PLUS -> Messages.get("devutils.encoder.url.spaces.plus");
        };
    }
}
