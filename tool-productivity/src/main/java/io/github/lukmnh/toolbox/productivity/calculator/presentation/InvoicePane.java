package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.productivity.calculator.domain.Invoice;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceLine;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceParty;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoicePricing;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceTotals;
import io.github.lukmnh.toolbox.productivity.calculator.domain.InvoiceType;
import java.util.List;
import java.util.Map;
import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Tab Invoice/Kuitansi: data dokumen, penjual & pelanggan, tabel item yang bisa diedit, diskon/pajak/catatan, lalu
 * total dan download PDF. Panjang teks dibatasi langsung di field sesuai batas domain.
 */
final class InvoicePane {

    private static final double NUMBER_FIELD_WIDTH = 200;
    private static final double PARTY_WIDTH = 320;
    private static final double QUANTITY_WIDTH = 80;
    private static final double PRICE_WIDTH = 160;
    private static final double AMOUNT_WIDTH = 150;
    private static final double PERCENT_WIDTH = 90;
    private static final int DETAILS_ROWS = 3;
    /** Angka uang/qty terpanjang yang masuk akal, termasuk pemisah ribuan dan desimal. */
    private static final int NUMBER_LENGTH = 30;

    private static final int PERCENT_LENGTH = 10;

    private final InvoiceViewModel viewModel;
    private final VBox lineRows = new VBox(ToolLayout.GAP);

    private InvoicePane(InvoiceViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static Node create(InvoiceViewModel viewModel) {
        return new InvoicePane(viewModel).build();
    }

    private Node build() {
        renderLines();
        viewModel.lines().addListener((ListChangeListener<InvoiceLineInput>) change -> {
            if (isStructural(change)) {
                renderLines();
            }
        });
        return new VBox(ToolLayout.SPACING, buildDocument(), buildItems(), buildTotals());
    }

    private VBox buildDocument() {
        TextField number = limited(new TextField(), viewModel.numberProperty(), Invoice.MAX_NUMBER_LENGTH);
        number.setPrefWidth(NUMBER_FIELD_WIDTH);
        number.setPromptText(Messages.get("productivity.calculator.invoice.number.prompt"));
        DatePicker date = new DatePicker();
        date.valueProperty().bindBidirectional(viewModel.dateProperty());
        HBox meta = ToolLayout.row(
                ToolLayout.labeled(
                        Messages.get("productivity.calculator.invoice.type"),
                        ToolLayout.segmented(
                                viewModel.typeProperty(),
                                List.of(InvoiceType.values()),
                                CalculatorMessages::invoiceType)),
                ToolLayout.labeled(Messages.get(InvoiceViewModel.NUMBER_KEY), number),
                ToolLayout.labeled(Messages.get("productivity.calculator.invoice.date"), date));
        meta.setSpacing(ToolLayout.SPACING);

        HBox parties = ToolLayout.row(
                party(InvoiceViewModel.SELLER_KEY, viewModel.sellerNameProperty(), viewModel.sellerDetailsProperty()),
                party(
                        InvoiceViewModel.CUSTOMER_KEY,
                        viewModel.customerNameProperty(),
                        viewModel.customerDetailsProperty()));
        parties.setSpacing(ToolLayout.SPACING);
        parties.setAlignment(Pos.TOP_LEFT);
        return ToolLayout.card(Messages.get("productivity.calculator.invoice.document"), List.of(), meta, parties);
    }

    private static VBox party(String titleKey, StringProperty name, StringProperty details) {
        TextField nameField = limited(new TextField(), name, InvoiceParty.MAX_NAME_LENGTH);
        nameField.setPromptText(Messages.get("productivity.calculator.invoice.party.name.prompt"));
        TextArea detailsArea = limited(new TextArea(), details, InvoiceParty.MAX_DETAILS_LENGTH);
        detailsArea.setPromptText(Messages.get("productivity.calculator.invoice.party.details.prompt"));
        detailsArea.setPrefRowCount(DETAILS_ROWS);
        detailsArea.setWrapText(true);
        VBox box = ToolLayout.labeled(Messages.get(titleKey), new VBox(ToolLayout.GAP, nameField, detailsArea));
        box.setPrefWidth(PARTY_WIDTH);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox buildItems() {
        Button add = ToolLayout.button(Messages.get("productivity.calculator.invoice.addLine"), viewModel::addLine);
        add.disableProperty().bind(viewModel.canAddLine().not());
        Label limit =
                ToolLayout.hint(Messages.format("productivity.calculator.invoice.lineLimit", InvoicePricing.MAX_LINES));

        TextField discount = limited(new TextField(), viewModel.discountProperty(), PERCENT_LENGTH);
        discount.setPrefWidth(PERCENT_WIDTH);
        discount.setPromptText(Messages.get("productivity.calculator.invoice.discount.prompt"));
        TextField tax = limited(new TextField(), viewModel.taxProperty(), PERCENT_LENGTH);
        tax.setPrefWidth(PERCENT_WIDTH);
        HBox rates = ToolLayout.row(
                ToolLayout.labeled(Messages.get(InvoiceViewModel.DISCOUNT_KEY), percentRow(discount)),
                ToolLayout.labeled(Messages.get(InvoiceViewModel.TAX_KEY), percentRow(tax)));
        rates.setSpacing(ToolLayout.SPACING);

        TextArea notes = limited(new TextArea(), viewModel.notesProperty(), Invoice.MAX_NOTES_LENGTH);
        notes.setPromptText(Messages.get("productivity.calculator.invoice.notes.prompt"));
        notes.setPrefRowCount(DETAILS_ROWS);
        notes.setWrapText(true);

        Label error = ToolLayout.inlineMessage(Tone.DANGER);
        error.textProperty().bind(viewModel.errorProperty());
        return ToolLayout.card(
                Messages.get("productivity.calculator.invoice.items"),
                List.of(),
                headerRow(),
                lineRows,
                ToolLayout.row(add, ToolLayout.spacer(), limit),
                rates,
                ToolLayout.labeled(Messages.get("productivity.calculator.invoice.notes"), notes),
                error);
    }

    private static HBox percentRow(TextField field) {
        return ToolLayout.row(field, new Label(Messages.get("productivity.calculator.percentSign")));
    }

    private static HBox headerRow() {
        Label description = caption("productivity.calculator.invoice.column.description");
        description.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(description, Priority.ALWAYS);
        Label quantity = caption(InvoiceViewModel.QUANTITY_KEY);
        quantity.setPrefWidth(QUANTITY_WIDTH);
        Label price = caption(InvoiceViewModel.UNIT_PRICE_KEY);
        price.setPrefWidth(PRICE_WIDTH);
        Label amount = caption("productivity.calculator.invoice.column.amount");
        amount.setPrefWidth(AMOUNT_WIDTH);
        amount.setAlignment(Pos.CENTER_RIGHT);
        // Spacer selebar tombol hapus supaya judul kolom sejajar dengan field di bawahnya
        Button placeholder = new Button(Messages.get("productivity.calculator.discount.remove"));
        placeholder.setVisible(false);
        return ToolLayout.row(description, quantity, price, amount, placeholder);
    }

    private static Label caption(String key) {
        Label label = new Label(Messages.get(key));
        label.getStyleClass().add("field-label");
        return label;
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
    private void renderLines() {
        lineRows.getChildren()
                .setAll(viewModel.lines().stream().<Node>map(this::lineRow).toList());
    }

    private HBox lineRow(InvoiceLineInput line) {
        TextField description =
                limited(new TextField(), line.descriptionProperty(), InvoiceLine.MAX_DESCRIPTION_LENGTH);
        description.setPromptText(Messages.get("productivity.calculator.invoice.column.description.prompt"));
        description.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(description, Priority.ALWAYS);
        TextField quantity = limited(new TextField(), line.quantityProperty(), NUMBER_LENGTH);
        quantity.setPrefWidth(QUANTITY_WIDTH);
        TextField price = limited(new TextField(), line.unitPriceProperty(), NUMBER_LENGTH);
        price.setPrefWidth(PRICE_WIDTH);
        price.setPromptText(Messages.get("productivity.calculator.invoice.column.unitPrice.prompt"));
        Label amount = ToolLayout.statText();
        amount.setPrefWidth(AMOUNT_WIDTH);
        amount.setAlignment(Pos.CENTER_RIGHT);
        amount.textProperty().bind(line.amountProperty());
        Button remove = ToolLayout.button(
                Messages.get("productivity.calculator.discount.remove"), () -> viewModel.removeLine(line));
        remove.disableProperty().bind(Bindings.size(viewModel.lines()).lessThanOrEqualTo(1));
        return ToolLayout.row(description, quantity, price, amount, remove);
    }

    private VBox buildTotals() {
        FlowPane tiles = new FlowPane(ToolLayout.SPACING, ToolLayout.SPACING);
        viewModel
                .totalsProperty()
                .addListener(
                        (observable, previous, current) -> tiles.getChildren().setAll(totalTiles(current)));
        tiles.getChildren().setAll(totalTiles(viewModel.totalsProperty().get()));

        HBox download = ToolLayout.downloadButtons(
                ToolLayout.button(Messages.get("productivity.calculator.download.pdf"), viewModel::downloadPdf),
                ToolLayout.button(Messages.get("productivity.calculator.saveAs.pdf"), viewModel::savePdfAs));
        download.disableProperty().bind(viewModel.canExport().not());
        Label exportError = ToolLayout.inlineMessage(Tone.DANGER);
        exportError.textProperty().bind(viewModel.exportErrorProperty());
        Button copy = ToolLayout.button(Messages.get("productivity.calculator.copySummary"), viewModel::copySummary);
        VBox card = ToolLayout.card(
                Messages.get("productivity.calculator.invoice.totals"),
                List.of(copy),
                tiles,
                exportError,
                ToolLayout.row(
                        ToolLayout.hint(Messages.get("productivity.calculator.invoice.pdfHint")),
                        ToolLayout.spacer(),
                        download));
        card.visibleProperty().bind(viewModel.totalsProperty().isNotNull());
        ToolLayout.collapseWhenHidden(card);
        return card;
    }

    /** Label tile ikut berubah (misalnya persen pajak), jadi tile dibangun ulang dari baris total. */
    private static List<Node> totalTiles(InvoiceTotals totals) {
        if (totals == null) {
            return List.of();
        }
        return CalculatorReports.totalRows(totals).entrySet().stream()
                .<Node>map((Map.Entry<String, String> row) ->
                        CalculatorView.tile(row.getKey(), Bindings.createStringBinding(row::getValue)))
                .toList();
    }

    /** Batasi panjang ketikan (dan paste) sesuai batas domain, lalu ikat dua arah ke property view model. */
    private static <T extends TextInputControl> T limited(T control, StringProperty value, int maxLength) {
        control.setTextFormatter(
                new TextFormatter<String>(change -> change.getControlNewText().length() <= maxLength ? change : null));
        control.textProperty().bindBidirectional(value);
        return control;
    }
}
