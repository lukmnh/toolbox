package io.github.lukmnh.toolbox.devutils.qr.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.ui.ToolLayout;
import io.github.lukmnh.toolbox.devutils.qr.application.ExportQrPngUseCase;
import io.github.lukmnh.toolbox.devutils.qr.application.GenerateQrUseCase;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrErrorCorrection;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrMatrix;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrRaster;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Layout QR Code Generator: isi per mode di kiri, opsi + preview + download di kanan. */
final class QrView {

    /** Sisi tampilan preview; gambar dirender minimal sebesar ini lalu diperkecil halus oleh ImageView. */
    private static final int PREVIEW_SIDE = 280;

    private static final double SIDE_COLUMN_WIDTH = 340;
    private static final double SPINNER_WIDTH = 80;
    private static final int TEXT_ROWS = 8;
    private static final int ADDRESS_ROWS = 3;

    private final QrViewModel viewModel;

    private QrView(QrViewModel viewModel) {
        this.viewModel = viewModel;
    }

    static Node create(QrViewModel viewModel) {
        return new QrView(viewModel).build();
    }

    private Node build() {
        VBox content = buildContentCard();
        HBox.setHgrow(content, Priority.ALWAYS);
        VBox side = new VBox(ToolLayout.SPACING, buildOptionsCard(), buildPreviewCard());
        side.setMinWidth(SIDE_COLUMN_WIDTH);
        side.setPrefWidth(SIDE_COLUMN_WIDTH);
        HBox columns = new HBox(ToolLayout.SPACING, content, side);

        VBox page = ToolLayout.page();
        page.getChildren()
                .addAll(
                        ToolLayout.header(
                                ToolCategory.DEV_UTILS,
                                Messages.get("devutils.qr.name"),
                                Messages.get("devutils.qr.hint")),
                        columns);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");
        return scroll;
    }

    private VBox buildContentCard() {
        TextArea text = ToolLayout.codeArea(true);
        text.setPromptText(Messages.get("devutils.qr.text.prompt"));
        text.setPrefRowCount(TEXT_ROWS);
        text.textProperty().bindBidirectional(viewModel.textProperty());
        showOnlyIn(text, QrMode.TEXT);

        TextField url = ToolLayout.codeField();
        url.setPromptText(Messages.get("devutils.qr.url.prompt"));
        url.textProperty().bindBidirectional(viewModel.urlProperty());
        showOnlyIn(url, QrMode.URL);

        GridPane vcard = buildVCardForm();
        showOnlyIn(vcard, QrMode.VCARD);

        return ToolLayout.card(
                Messages.get("devutils.qr.content"),
                List.of(),
                ToolLayout.segmented(
                        viewModel.modeProperty(), List.of(QrMode.values()), mode -> Messages.get(mode.labelKey())),
                text,
                url,
                vcard);
    }

    private GridPane buildVCardForm() {
        GridPane grid = new GridPane();
        grid.setHgap(ToolLayout.SPACING);
        grid.setVgap(ToolLayout.GAP);
        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        half.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(half, half);
        grid.add(field("devutils.qr.vcard.name", viewModel.nameProperty()), 0, 0);
        grid.add(field("devutils.qr.vcard.organization", viewModel.organizationProperty()), 1, 0);
        grid.add(field("devutils.qr.vcard.title", viewModel.titleProperty()), 0, 1);
        grid.add(field("devutils.qr.vcard.phone", viewModel.phoneProperty()), 1, 1);
        grid.add(field("devutils.qr.vcard.email", viewModel.emailProperty()), 0, 2);
        grid.add(field("devutils.qr.vcard.website", viewModel.websiteProperty()), 1, 2);

        TextArea address = new TextArea();
        address.setPrefRowCount(ADDRESS_ROWS);
        address.setWrapText(true);
        address.textProperty().bindBidirectional(viewModel.addressProperty());
        grid.add(ToolLayout.labeled(Messages.get("devutils.qr.vcard.address"), address), 0, 3, 2, 1);
        return grid;
    }

    private static VBox field(String labelKey, StringProperty value) {
        TextField field = new TextField();
        field.textProperty().bindBidirectional(value);
        return ToolLayout.labeled(Messages.get(labelKey), field);
    }

    private VBox buildOptionsCard() {
        HBox errorCorrection = ToolLayout.segmented(
                viewModel.errorCorrectionProperty(), List.of(QrErrorCorrection.values()), QrErrorCorrection::name);
        Tooltip.install(errorCorrection, new Tooltip(Messages.get("devutils.qr.errorCorrection.tooltip")));

        ChoiceBox<Integer> size = new ChoiceBox<>();
        size.getItems().setAll(ExportQrPngUseCase.SIZES);
        size.setConverter(ToolLayout.labels(px -> Messages.format("devutils.qr.size.value", String.valueOf(px))));
        size.valueProperty().bindBidirectional(viewModel.sizeProperty());

        Spinner<Integer> margin = new Spinner<>(
                GenerateQrUseCase.MIN_MARGIN,
                GenerateQrUseCase.MAX_MARGIN,
                viewModel.marginProperty().get());
        margin.setPrefWidth(SPINNER_WIDTH);
        margin.getValueFactory().valueProperty().bindBidirectional(viewModel.marginProperty());

        HBox sizeRow = ToolLayout.row(
                ToolLayout.labeled(Messages.get("devutils.qr.size"), size),
                ToolLayout.labeled(Messages.get("devutils.qr.margin"), margin));
        sizeRow.setSpacing(ToolLayout.SPACING);
        return ToolLayout.card(
                Messages.get("devutils.qr.options"),
                List.of(),
                ToolLayout.labeled(Messages.get("devutils.qr.errorCorrection"), errorCorrection),
                sizeRow);
    }

    private VBox buildPreviewCard() {
        ImageView image = new ImageView();
        image.setFitWidth(PREVIEW_SIDE);
        image.setFitHeight(PREVIEW_SIDE);
        image.setPreserveRatio(true);
        image.setSmooth(true);
        image.imageProperty()
                .bind(Bindings.createObjectBinding(
                        () -> toImage(viewModel.matrixProperty().get()), viewModel.matrixProperty()));
        StackPane preview = new StackPane(image);
        preview.getStyleClass().add("qr-preview");
        preview.setMinSize(PREVIEW_SIDE, PREVIEW_SIDE);
        preview.visibleProperty().bind(viewModel.matrixProperty().isNotNull());
        ToolLayout.collapseWhenHidden(preview);

        Label message = ToolLayout.inlineMessage(viewModel.messageToneProperty().get());
        message.textProperty().bind(viewModel.messageProperty());
        viewModel.messageToneProperty().addListener((observable, previous, tone) -> ToolLayout.setTone(message, tone));

        Label stats = ToolLayout.statText();
        stats.textProperty()
                .bind(Bindings.createStringBinding(
                        viewModel::statsText, viewModel.matrixProperty(), viewModel.sizeProperty()));

        BooleanBinding missing = viewModel.matrixProperty().isNull();
        Button copy = ToolLayout.button(Messages.get("devutils.qr.copyPayload"), viewModel::copyPayload);
        copy.setTooltip(new Tooltip(Messages.get("devutils.qr.copyPayload.tooltip")));
        Button download = new Button(Messages.get("download.button"));
        download.setOnAction(event -> viewModel.download());
        Button saveAs = new Button(Messages.get("download.saveAs.button"));
        saveAs.setOnAction(event -> viewModel.saveAs());
        for (Button button : List.of(copy, download, saveAs)) {
            button.disableProperty().bind(missing);
        }
        return ToolLayout.card(
                Messages.get("devutils.qr.preview"),
                List.of(),
                preview,
                message,
                stats,
                ToolLayout.row(copy, ToolLayout.spacer(), ToolLayout.downloadButtons(download, saveAs)));
    }

    private void showOnlyIn(Node node, QrMode mode) {
        node.visibleProperty().bind(viewModel.modeProperty().isEqualTo(mode));
        ToolLayout.collapseWhenHidden(node);
    }

    /** Render langsung ke {@link WritableImage} (tanpa Swing); skala bulat supaya modul tetap tajam. */
    private static Image toImage(QrMatrix matrix) {
        if (matrix == null) {
            return null;
        }
        int scale = Math.max(1, (PREVIEW_SIDE + matrix.dimension() - 1) / matrix.dimension());
        QrRaster raster = matrix.rasterize(matrix.dimension() * scale);
        WritableImage image = new WritableImage(raster.side(), raster.side());
        image.getPixelWriter()
                .setPixels(
                        0,
                        0,
                        raster.side(),
                        raster.side(),
                        PixelFormat.getIntArgbInstance(),
                        raster.argb(),
                        0,
                        raster.side());
        return image;
    }
}
