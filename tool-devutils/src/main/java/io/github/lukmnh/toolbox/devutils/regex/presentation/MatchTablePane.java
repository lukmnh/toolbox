package io.github.lukmnh.toolbox.devutils.regex.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.devutils.regex.application.RegexRun;
import io.github.lukmnh.toolbox.devutils.regex.domain.GroupMatch;
import io.github.lukmnh.toolbox.devutils.regex.domain.RegexMatch;
import io.github.lukmnh.toolbox.devutils.shared.presentation.DevLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Tab Match: tabel #, posisi, nilai, dan satu kolom per grup (nama grup bila ada). */
final class MatchTablePane {

    private static final double NUMBER_WIDTH = 56;
    private static final double POSITION_WIDTH = 72;
    private static final double VALUE_WIDTH = 220;
    private static final double GROUP_WIDTH = 140;

    /** Baris tabel; nomor disimpan supaya tidak perlu {@code indexOf} per sel. */
    record MatchRow(int number, RegexMatch match) {}

    private final RegexViewModel viewModel;
    private final TableView<MatchRow> table = new TableView<>();
    private final Label summary = DevLayout.statText();

    private MatchTablePane(RegexViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static Node create(RegexViewModel viewModel) {
        return new MatchTablePane(viewModel).build();
    }

    private Node build() {
        table.setPlaceholder(new Label(Messages.get("devutils.regex.match.none")));
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        viewModel.evaluationProperty().addListener((observable, previous, current) -> refresh(current));
        refresh(null);
        VBox box = new VBox(DevLayout.GAP, summary, table);
        box.setPadding(new Insets(DevLayout.GAP));
        return box;
    }

    private void refresh(Evaluation evaluation) {
        RegexRun run = evaluation == null ? null : evaluation.run().orElse(null);
        table.getColumns().setAll(columns(run == null ? List.of("") : run.groupNames()));
        if (run == null) {
            table.getItems().clear();
            summary.setText("");
            return;
        }
        List<MatchRow> rows = new ArrayList<>(run.scan().matches().size());
        for (int index = 0; index < run.scan().matches().size(); index++) {
            rows.add(new MatchRow(index + 1, run.scan().matches().get(index)));
        }
        table.getItems().setAll(rows);
        summary.setText(Messages.format(
                "devutils.regex.match.summary",
                RegexMessages.number(rows.size()),
                RegexMessages.number(run.scan().totalCount())));
    }

    private static List<TableColumn<MatchRow, String>> columns(List<String> groupNames) {
        List<TableColumn<MatchRow, String>> columns = new ArrayList<>();
        columns.add(
                column(Messages.get("devutils.regex.match.number"), NUMBER_WIDTH, row -> String.valueOf(row.number())));
        columns.add(column(
                Messages.get("devutils.regex.match.start"),
                POSITION_WIDTH,
                row -> String.valueOf(row.match().start())));
        columns.add(column(
                Messages.get("devutils.regex.match.end"),
                POSITION_WIDTH,
                row -> String.valueOf(row.match().end())));
        columns.add(column(
                Messages.get("devutils.regex.match.value"),
                VALUE_WIDTH,
                row -> row.match().value()));
        for (int number = 1; number < groupNames.size(); number++) {
            int group = number;
            String title = groupNames.get(number).isEmpty()
                    ? Messages.format("devutils.regex.match.group", String.valueOf(number))
                    : Messages.format(
                            "devutils.regex.match.namedGroup", String.valueOf(number), groupNames.get(number));
            columns.add(column(title, GROUP_WIDTH, row -> groupValue(row.match(), group)));
        }
        return columns;
    }

    private static String groupValue(RegexMatch match, int number) {
        return match.groups().stream()
                .filter(group -> group.number() == number)
                .findFirst()
                .filter(GroupMatch::matched)
                .map(GroupMatch::value)
                .orElse(Messages.get("devutils.regex.match.unmatched"));
    }

    private static TableColumn<MatchRow, String> column(String title, double width, Function<MatchRow, String> value) {
        TableColumn<MatchRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setSortable(false);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.getStyleClass().add("regex-match-column");
        return column;
    }
}
