package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.calculator.domain.TaxMode;
import io.github.lukmnh.toolbox.productivity.calculator.domain.TaxResult;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Tab Pajak & PPN: nominal + tarif + mode (sebelum/termasuk pajak), lalu DPP, pajak, dan total. */
final class TaxPane {

    private static final double MONEY_FIELD_WIDTH = 220;
    private static final double RATE_FIELD_WIDTH = 90;

    private final TaxViewModel viewModel;

    private TaxPane(TaxViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static Node create(TaxViewModel viewModel) {
        return new TaxPane(viewModel).build();
    }

    private Node build() {
        return new VBox(ToolLayout.SPACING, buildInput(), buildResult());
    }

    private VBox buildInput() {
        TextField amount = new TextField();
        amount.setPrefWidth(MONEY_FIELD_WIDTH);
        amount.setMaxWidth(MONEY_FIELD_WIDTH);
        amount.setPromptText(Messages.get("productivity.calculator.tax.amount.prompt"));
        amount.textProperty().bindBidirectional(viewModel.amountProperty());

        TextField rate = new TextField();
        rate.setPrefWidth(RATE_FIELD_WIDTH);
        rate.textProperty().bindBidirectional(viewModel.rateProperty());
        HBox rateRow = ToolLayout.row(rate, new Label(Messages.get("productivity.calculator.percentSign")));

        Label modeHint = ToolLayout.hint("");
        modeHint.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> Messages.get("productivity.calculator.tax.mode."
                                + viewModel.modeProperty().get().name().toLowerCase(Locale.ROOT)
                                + ".hint"),
                        viewModel.modeProperty()));

        Label error = ToolLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.errorProperty());
        HBox fields = ToolLayout.row(
                ToolLayout.labeled(Messages.get(TaxViewModel.AMOUNT_KEY), amount),
                ToolLayout.labeled(Messages.get(TaxViewModel.RATE_KEY), rateRow));
        fields.setSpacing(ToolLayout.SPACING);
        return ToolLayout.card(
                Messages.get("productivity.calculator.tax.input"),
                List.of(),
                ToolLayout.labeled(
                        Messages.get("productivity.calculator.tax.mode"),
                        ToolLayout.segmented(
                                viewModel.modeProperty(), List.of(TaxMode.values()), CalculatorMessages::taxMode)),
                modeHint,
                fields,
                error);
    }

    private VBox buildResult() {
        FlowPane tiles = new FlowPane(ToolLayout.SPACING, ToolLayout.SPACING);
        tiles.getChildren()
                .addAll(
                        CalculatorView.tile(
                                Messages.get("productivity.calculator.tax.base"),
                                resultText(result -> CalculatorMessages.money(result.base()))),
                        CalculatorView.tile(
                                Bindings.createStringBinding(
                                        () -> viewModel.resultProperty().get() == null
                                                ? ""
                                                : Messages.format(
                                                        "productivity.calculator.tax.taxAmount",
                                                        CalculatorMessages.percent(viewModel
                                                                .resultProperty()
                                                                .get()
                                                                .ratePercent())),
                                        viewModel.resultProperty()),
                                resultText(result -> CalculatorMessages.money(result.tax()))),
                        CalculatorView.tile(
                                Messages.get("productivity.calculator.tax.total"),
                                resultText(result -> CalculatorMessages.money(result.total()))));
        Button copy = ToolLayout.button(Messages.get("productivity.calculator.copySummary"), viewModel::copySummary);
        VBox card = ToolLayout.card(Messages.get("productivity.calculator.result"), List.of(copy), tiles);
        card.visibleProperty().bind(viewModel.resultProperty().isNotNull());
        ToolLayout.collapseWhenHidden(card);
        return card;
    }

    private StringBinding resultText(Function<TaxResult, String> text) {
        return Bindings.createStringBinding(
                () -> viewModel.resultProperty().get() == null
                        ? ""
                        : text.apply(viewModel.resultProperty().get()),
                viewModel.resultProperty());
    }
}
