package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.calculator.domain.AmortizationRow;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentMethod;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InstallmentSchedule;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Tab Simulasi Cicilan: input pinjaman, ringkasan, lalu tabel amortisasi (virtual) + download CSV. */
final class InstallmentPane {

    private static final double FIELD_WIDTH = 200;
    private static final double SHORT_FIELD_WIDTH = 110;
    private static final double TABLE_HEIGHT = 380;
    private static final String NUMBER_COLUMN_CLASS = "number-column";

    private final InstallmentViewModel viewModel;

    private InstallmentPane(InstallmentViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static Node create(InstallmentViewModel viewModel) {
        return new InstallmentPane(viewModel).build();
    }

    private Node build() {
        return new VBox(ToolLayout.SPACING, buildInput(), buildSummary(), buildSchedule());
    }

    private VBox buildInput() {
        HBox loanRow = ToolLayout.row(
                field(InstallmentViewModel.PRINCIPAL_KEY, viewModel.principalProperty(), FIELD_WIDTH),
                field(InstallmentViewModel.TENOR_KEY, viewModel.tenorProperty(), SHORT_FIELD_WIDTH),
                field(InstallmentViewModel.RATE_KEY, viewModel.rateProperty(), SHORT_FIELD_WIDTH));
        loanRow.setSpacing(ToolLayout.SPACING);

        Label methodHint = ToolLayout.hint("");
        methodHint
                .textProperty()
                .bind(Bindings.createStringBinding(
                        () -> CalculatorMessages.methodHint(
                                viewModel.methodProperty().get()),
                        viewModel.methodProperty()));
        VBox methodBox = ToolLayout.labeled(
                Messages.get("productivity.calculator.installment.method"),
                ToolLayout.segmented(
                        viewModel.methodProperty(), List.of(InstallmentMethod.values()), CalculatorMessages::method));

        HBox feeRow = ToolLayout.row(
                ToolLayout.segmented(
                        viewModel.feeModeProperty(),
                        List.of(FeeMode.values()),
                        mode -> Messages.get("productivity.calculator.installment.fee."
                                + mode.name().toLowerCase(Locale.ROOT))),
                textField(viewModel.feeProperty(), "productivity.calculator.installment.fee.prompt", FIELD_WIDTH));

        Label error = ToolLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.errorProperty());
        return ToolLayout.card(
                Messages.get("productivity.calculator.installment.input"),
                List.of(),
                loanRow,
                methodBox,
                methodHint,
                ToolLayout.labeled(Messages.get(InstallmentViewModel.FEE_KEY), feeRow),
                error);
    }

    private VBox buildSummary() {
        Node first = CalculatorView.tile(
                Bindings.createStringBinding(
                        () -> viewModel.scheduleProperty().get() == null
                                ? ""
                                : Messages.get(InstallmentViewModel.firstPaymentKey(
                                        viewModel.scheduleProperty().get())),
                        viewModel.scheduleProperty()),
                scheduleText(schedule -> CalculatorMessages.money(schedule.firstPayment())));
        Node last = CalculatorView.tile(
                Messages.get("productivity.calculator.installment.lastPayment"),
                scheduleText(schedule -> CalculatorMessages.money(schedule.lastPayment())));
        last.visibleProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> viewModel.scheduleProperty().get() != null
                                && InstallmentViewModel.hasDifferentLastPayment(
                                        viewModel.scheduleProperty().get()),
                        viewModel.scheduleProperty()));
        ToolLayout.collapseWhenHidden(last);

        FlowPane tiles = new FlowPane(ToolLayout.SPACING, ToolLayout.SPACING);
        tiles.getChildren()
                .addAll(
                        first,
                        last,
                        CalculatorView.tile(
                                Messages.get("productivity.calculator.installment.totalInterest"),
                                scheduleText(schedule -> CalculatorMessages.money(schedule.totalInterest()))),
                        CalculatorView.tile(
                                Messages.get("productivity.calculator.installment.feeAmount"),
                                scheduleText(schedule -> CalculatorMessages.money(schedule.fee()))),
                        CalculatorView.tile(
                                Messages.get("productivity.calculator.installment.totalCost"),
                                scheduleText(schedule -> CalculatorMessages.money(schedule.totalCost()))));

        Label disclaimer = ToolLayout.inlineMessage(Tone.INFO);
        disclaimer.setText(Messages.get("productivity.calculator.installment.disclaimer"));
        Button copy = ToolLayout.button(Messages.get("productivity.calculator.copySummary"), viewModel::copySummary);
        VBox card = ToolLayout.card(
                Messages.get("productivity.calculator.result"),
                List.of(copy),
                tiles,
                ToolLayout.hint(Messages.get("productivity.calculator.installment.rounding")),
                disclaimer);
        card.visibleProperty().bind(viewModel.scheduleProperty().isNotNull());
        ToolLayout.collapseWhenHidden(card);
        return card;
    }

    private VBox buildSchedule() {
        TableView<AmortizationRow> table = new TableView<>(viewModel.rows());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(TABLE_HEIGHT);
        table.setPlaceholder(new Label(Messages.get("productivity.calculator.schedule.empty")));
        List<String> keys = InstallmentViewModel.COLUMN_KEYS;
        table.getColumns()
                .setAll(List.of(
                        column(keys.get(0), row -> String.valueOf(row.month()), false),
                        column(keys.get(1), row -> CalculatorMessages.money(row.payment()), true),
                        column(keys.get(2), row -> CalculatorMessages.money(row.principalPart()), true),
                        column(keys.get(3), row -> CalculatorMessages.money(row.interest()), true),
                        column(keys.get(4), row -> CalculatorMessages.money(row.remaining()), true)));

        HBox download = ToolLayout.downloadButtons(viewModel::download, viewModel::saveAs);
        download.disableProperty().bind(viewModel.canExport().not());
        VBox card = ToolLayout.card(
                Messages.get("productivity.calculator.schedule"),
                List.of(),
                table,
                ToolLayout.row(
                        ToolLayout.hint(Messages.get("productivity.calculator.schedule.csvHint")),
                        ToolLayout.spacer(),
                        download));
        card.visibleProperty().bind(viewModel.scheduleProperty().isNotNull());
        ToolLayout.collapseWhenHidden(card);
        return card;
    }

    private static TableColumn<AmortizationRow, String> column(
            String titleKey, Function<AmortizationRow, String> value, boolean numeric) {
        TableColumn<AmortizationRow, String> column = new TableColumn<>(Messages.get(titleKey));
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setSortable(false);
        if (numeric) {
            column.getStyleClass().add(NUMBER_COLUMN_CLASS);
        }
        return column;
    }

    private StringBinding scheduleText(Function<InstallmentSchedule, String> text) {
        return Bindings.createStringBinding(
                () -> viewModel.scheduleProperty().get() == null
                        ? ""
                        : text.apply(viewModel.scheduleProperty().get()),
                viewModel.scheduleProperty());
    }

    private static VBox field(String labelKey, StringProperty value, double width) {
        return ToolLayout.labeled(Messages.get(labelKey), textField(value, labelKey + ".prompt", width));
    }

    private static TextField textField(StringProperty value, String promptKey, double width) {
        TextField field = new TextField();
        field.setPrefWidth(width);
        field.setPromptText(Messages.get(promptKey));
        field.textProperty().bindBidirectional(value);
        return field;
    }
}
