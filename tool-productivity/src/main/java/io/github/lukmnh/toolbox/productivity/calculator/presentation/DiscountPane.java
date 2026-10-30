package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.calculator.domain.DiscountResult;
import io.github.lukmnh.toolbox.productivity.calculator.domain.DiscountStep;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Tab Diskon Ganda: harga, daftar diskon bertingkat, potongan nominal, lalu hasil + rincian per tahap. */
final class DiscountPane {

    private static final double PERCENT_FIELD_WIDTH = 110;
    private static final double MONEY_FIELD_WIDTH = 220;

    private final DiscountViewModel viewModel;
    private final VBox discountRows = new VBox(ToolLayout.GAP);

    private DiscountPane(DiscountViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static Node create(DiscountViewModel viewModel) {
        return new DiscountPane(viewModel).build();
    }

    private Node build() {
        renderDiscountRows();
        viewModel.discounts().addListener((ListChangeListener<StringProperty>) change -> {
            if (isStructural(change)) {
                renderDiscountRows();
            }
        });
        return new VBox(ToolLayout.SPACING, buildInput(), buildResult());
    }

    private VBox buildInput() {
        TextField price = moneyField(viewModel.priceProperty(), "productivity.calculator.discount.price.prompt");
        TextField deduction =
                moneyField(viewModel.deductionProperty(), "productivity.calculator.discount.deduction.prompt");
        Button add = ToolLayout.button(Messages.get("productivity.calculator.discount.add"), viewModel::addDiscount);
        add.disableProperty().bind(viewModel.canAddDiscount().not());
        Label error = ToolLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.errorProperty());
        return ToolLayout.card(
                Messages.get("productivity.calculator.discount.input"),
                List.of(),
                ToolLayout.labeled(Messages.get(DiscountViewModel.PRICE_KEY), price),
                ToolLayout.labeled(
                        Messages.get("productivity.calculator.discount.percents"),
                        new VBox(ToolLayout.GAP, discountRows, add)),
                ToolLayout.labeled(Messages.get(DiscountViewModel.DEDUCTION_KEY), deduction),
                error);
    }

    private static boolean isStructural(ListChangeListener.Change<?> change) {
        while (change.next()) {
            if (change.wasAdded() || change.wasRemoved()) {
                return true;
            }
        }
        return false;
    }

    /** Hanya dipanggil saat baris ditambah/dihapus; mengetik di baris tidak membangun ulang field. */
    private void renderDiscountRows() {
        List<StringProperty> rows = viewModel.discounts();
        discountRows
                .getChildren()
                .setAll(IntStream.range(0, rows.size())
                        .<Node>mapToObj(index -> discountRow(index + 1, rows.get(index)))
                        .toList());
    }

    private HBox discountRow(int number, StringProperty value) {
        TextField field = new TextField();
        field.setPrefWidth(PERCENT_FIELD_WIDTH);
        field.setPromptText(Messages.get("productivity.calculator.discount.percent.prompt"));
        field.textProperty().bindBidirectional(value);
        Label caption = ToolLayout.statText();
        caption.setText(Messages.format("productivity.calculator.discount.row", number));
        Label percentSign = new Label(Messages.get("productivity.calculator.percentSign"));
        Button remove = ToolLayout.button(
                Messages.get("productivity.calculator.discount.remove"), () -> viewModel.removeDiscount(value));
        return ToolLayout.row(caption, field, percentSign, remove);
    }

    private VBox buildResult() {
        FlowPane tiles = new FlowPane(ToolLayout.SPACING, ToolLayout.SPACING);
        tiles.getChildren()
                .addAll(
                        CalculatorView.tile(
                                Messages.get("productivity.calculator.discount.final"),
                                resultText(result -> CalculatorMessages.money(result.finalPrice()))),
                        CalculatorView.tile(
                                Messages.get("productivity.calculator.discount.saving"),
                                resultText(result -> CalculatorMessages.money(result.totalSaving()))),
                        CalculatorView.tile(
                                Messages.get("productivity.calculator.discount.effective"),
                                resultText(result -> CalculatorMessages.percent(result.effectiveDiscountPercent()))));
        Label clamped = ToolLayout.inlineMessage(Tone.WARNING);
        clamped.textProperty()
                .bind(resultText(
                        result -> result.clamped() ? Messages.get("productivity.calculator.discount.clamped") : ""));
        VBox steps = new VBox(4);
        viewModel
                .resultProperty()
                .addListener(
                        (observable, previous, current) -> steps.getChildren().setAll(stepLabels(current)));

        Button copy = ToolLayout.button(Messages.get("productivity.calculator.copySummary"), viewModel::copySummary);
        VBox card = ToolLayout.card(
                Messages.get("productivity.calculator.result"),
                List.of(copy),
                tiles,
                clamped,
                ToolLayout.labeled(Messages.get("productivity.calculator.discount.steps"), steps));
        card.visibleProperty().bind(viewModel.resultProperty().isNotNull());
        ToolLayout.collapseWhenHidden(card);
        return card;
    }

    private static List<Node> stepLabels(DiscountResult result) {
        if (result == null || result.steps().isEmpty()) {
            return List.of(ToolLayout.hint(Messages.get("productivity.calculator.discount.noSteps")));
        }
        List<DiscountStep> steps = result.steps();
        return IntStream.range(0, steps.size())
                .<Node>mapToObj(index -> new Label(CalculatorMessages.step(index + 1, steps.get(index))))
                .toList();
    }

    private StringBinding resultText(Function<DiscountResult, String> text) {
        return Bindings.createStringBinding(
                () -> viewModel.resultProperty().get() == null
                        ? ""
                        : text.apply(viewModel.resultProperty().get()),
                viewModel.resultProperty());
    }

    private static TextField moneyField(StringProperty value, String promptKey) {
        TextField field = new TextField();
        field.setPrefWidth(MONEY_FIELD_WIDTH);
        field.setMaxWidth(MONEY_FIELD_WIDTH);
        field.setPromptText(Messages.get(promptKey));
        field.textProperty().bindBidirectional(value);
        return field;
    }
}
