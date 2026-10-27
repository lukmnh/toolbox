package io.github.lukmnh.toolbox.pdf.sign.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.TaskStatusBar;
import io.github.lukmnh.toolbox.pdf.shared.presentation.PdfFileChooser;
import io.github.lukmnh.toolbox.pdf.shared.presentation.ToolLayout;
import io.github.lukmnh.toolbox.pdf.sign.domain.EditDefaults;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Layout tool Isi & TTD: toolbar, kanvas halaman, dan panel form. */
public final class SignView extends BorderPane {

    private static final double PAGE_FIELD_WIDTH = 56;
    private static final double FONT_SPINNER_WIDTH = 90;
    private static final double SIGNATURE_THUMB_WIDTH = 72;
    private static final double SIGNATURE_THUMB_HEIGHT = 28;
    private static final int PERCENT = 100;
    private static final double SPACING = 8;

    private final SignViewModel viewModel;
    private final Window owner;
    private final PageCanvas canvas;

    public SignView(SignViewModel viewModel, Window owner) {
        this.viewModel = viewModel;
        this.owner = owner;
        this.canvas = new PageCanvas(viewModel, this::askForSignature);
        getStyleClass().addAll("tool-view", "sign-view");

        setTop(new VBox(fileToolbar(), editToolbar()));
        setCenter(center());
        setRight(new FormFieldPanel(viewModel));
        TaskStatusBar status = new TaskStatusBar();
        status.setPadding(new Insets(SPACING));
        viewModel.currentTaskProperty().addListener((observable, previous, task) -> status.track(task));
        setBottom(status);

        ToolLayout.acceptPdfDrops(this, files -> openIfConfirmed(files.getFirst()), viewModel::warn);
        // Begitu halaman pertama selesai dirender, sesuaikan zoom ke lebar area
        viewModel.pageImageProperty().addListener((observable, previous, image) -> {
            if (previous == null && image != null) {
                canvas.layout();
                canvas.fitWidth();
            }
        });
    }

    // ---------- Toolbar ----------

    private HBox fileToolbar() {
        Button open = new Button(Messages.get("pdf.sign.open"));
        open.setOnAction(event -> PdfFileChooser.chooseOne(owner).ifPresent(this::openIfConfirmed));

        Button download = new Button(Messages.get("download.button"));
        download.setOnAction(event -> viewModel.download());
        Button saveAs = new Button(Messages.get("download.saveAs.button"));
        saveAs.setOnAction(event -> viewModel.saveAs());
        HBox downloads = ToolLayout.downloadButtons(download, saveAs);
        downloads.disableProperty().bind(viewModel.sourceProperty().isNull());

        HBox bar = toolbar(
                open,
                new Separator(),
                pageNavigation(),
                new Separator(),
                zoomControls(),
                ToolLayout.spacer(),
                downloads);
        bar.getStyleClass().add("tool-toolbar");
        return bar;
    }

    private HBox pageNavigation() {
        Button previous = new Button("◀");
        previous.setTooltip(new Tooltip(Messages.get("pdf.sign.page.previous")));
        previous.setOnAction(
                event -> viewModel.goToPage(viewModel.pageIndexProperty().get() - 1));
        Button next = new Button("▶");
        next.setTooltip(new Tooltip(Messages.get("pdf.sign.page.next")));
        next.setOnAction(
                event -> viewModel.goToPage(viewModel.pageIndexProperty().get() + 1));

        TextField pageField = new TextField();
        pageField.setPrefWidth(PAGE_FIELD_WIDTH);
        // Dengarkan pageCount juga: saat dokumen baru dibuka indeks tetap 0 sehingga listener indeks saja tidak
        // terpicu. Bukan binding, karena field harus tetap bisa diketik user.
        InvalidationListener syncPageField = observable -> pageField.setText(
                viewModel.pageCountProperty().get() == 0
                        ? ""
                        : Integer.toString(viewModel.pageIndexProperty().get() + 1));
        viewModel.pageIndexProperty().addListener(syncPageField);
        viewModel.pageCountProperty().addListener(syncPageField);
        viewModel.sourceProperty().addListener(syncPageField);
        pageField.setOnAction(event -> {
            parsePage(pageField.getText());
            syncPageField.invalidated(viewModel.pageIndexProperty());
        });

        Label total = new Label();
        total.textProperty().bind(Bindings.createStringBinding(viewModel::pageLabel, viewModel.pageCountProperty()));

        previous.disableProperty().bind(viewModel.pageIndexProperty().lessThanOrEqualTo(0));
        next.disableProperty()
                .bind(viewModel
                        .pageIndexProperty()
                        .greaterThanOrEqualTo(viewModel.pageCountProperty().subtract(1)));
        HBox navigation = toolbar(previous, pageField, total, next);
        navigation.disableProperty().bind(viewModel.sourceProperty().isNull());
        return navigation;
    }

    private void parsePage(String text) {
        try {
            viewModel.goToPage(Integer.parseInt(text.strip()) - 1);
        } catch (NumberFormatException e) {
            // Input bukan angka: kembalikan ke halaman sekarang tanpa error dialog
            viewModel.warn(Messages.format("pdf.sign.page.invalid", text));
        }
    }

    private HBox zoomControls() {
        Button zoomOut = new Button("−");
        zoomOut.setOnAction(event -> canvas.zoomOut());
        Button zoomIn = new Button("+");
        zoomIn.setOnAction(event -> canvas.zoomIn());
        Button fit = new Button(Messages.get("pdf.sign.zoom.fit"));
        fit.setOnAction(event -> canvas.fitWidth());
        Label percent = new Label();
        percent.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> Math.round(canvas.zoomProperty().get() * PERCENT) + "%", canvas.zoomProperty()));
        HBox zoom = toolbar(zoomOut, percent, zoomIn, fit);
        zoom.disableProperty().bind(viewModel.sourceProperty().isNull());
        return zoom;
    }

    private HBox editToolbar() {
        ToggleGroup modes = new ToggleGroup();
        HBox modeButtons = toolbar();
        for (EditMode mode : EditMode.values()) {
            ToggleButton button = new ToggleButton(Messages.get(mode.messageKey()));
            button.setUserData(mode);
            button.setToggleGroup(modes);
            button.setSelected(mode == viewModel.modeProperty().get());
            modeButtons.getChildren().add(button);
        }
        modes.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                modes.selectToggle(previous);
                return;
            }
            canvas.stopEditing();
            viewModel.modeProperty().set((EditMode) selected.getUserData());
        });

        HBox bar = toolbar(modeButtons, new Separator(), fontSizeControl(), new Separator(), signatureControls());
        bar.getStyleClass().add("tool-toolbar");
        bar.disableProperty().bind(viewModel.sourceProperty().isNull());
        return bar;
    }

    private HBox fontSizeControl() {
        Spinner<Double> spinner = new Spinner<>();
        SpinnerValueFactory.DoubleSpinnerValueFactory factory = new SpinnerValueFactory.DoubleSpinnerValueFactory(
                EditDefaults.MIN_FONT_SIZE,
                EditDefaults.MAX_FONT_SIZE,
                viewModel.fontSizeProperty().get(),
                EditDefaults.FONT_SIZE_STEP);
        spinner.setValueFactory(factory);
        spinner.setEditable(true);
        spinner.setPrefWidth(FONT_SPINNER_WIDTH);
        factory.valueProperty().addListener((observable, previous, value) -> {
            if (value != null) {
                viewModel.fontSizeProperty().set(value);
            }
        });
        viewModel
                .fontSizeProperty()
                .addListener((observable, previous, value) -> factory.setValue(value.doubleValue()));
        return toolbar(new Label(Messages.get("pdf.sign.fontSize")), spinner);
    }

    private HBox signatureControls() {
        Button draw = new Button(Messages.get("pdf.sign.signature.draw"));
        draw.setOnAction(event -> drawSignature());
        Button upload = new Button(Messages.get("pdf.sign.signature.upload"));
        upload.setOnAction(event -> uploadSignature());
        ImageView thumbnail = new ImageView();
        thumbnail.imageProperty().bind(viewModel.signaturePreviewProperty());
        thumbnail.setFitWidth(SIGNATURE_THUMB_WIDTH);
        thumbnail.setFitHeight(SIGNATURE_THUMB_HEIGHT);
        thumbnail.setPreserveRatio(true);
        StackPane thumbnailFrame = new StackPane(thumbnail);
        thumbnailFrame.getStyleClass().add("signature-thumbnail");
        Tooltip.install(thumbnailFrame, new Tooltip(Messages.get("pdf.sign.signature.current")));
        return toolbar(draw, upload, thumbnailFrame);
    }

    private static HBox toolbar(Node... children) {
        HBox box = new HBox(SPACING, children);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(SPACING / 2, SPACING, SPACING / 2, SPACING));
        return box;
    }

    // ---------- Area tengah ----------

    private StackPane center() {
        VBox placeholder = ToolLayout.dropZone(
                Messages.get("pdf.sign.drop"),
                () -> PdfFileChooser.chooseOne(owner).ifPresent(this::openIfConfirmed));
        placeholder.visibleProperty().bind(viewModel.sourceProperty().isNull());
        canvas.visibleProperty().bind(viewModel.sourceProperty().isNotNull());
        StackPane center = new StackPane(canvas, placeholder);
        StackPane.setMargin(placeholder, new Insets(SPACING * 3));
        return center;
    }

    // ---------- Aksi ----------

    void openIfConfirmed(Path file) {
        if (viewModel.confirmDiscard()) {
            viewModel.openFile(file);
        }
    }

    private void drawSignature() {
        new SignatureDialog(owner).showAndWait().ifPresent(viewModel::saveDrawnSignature);
    }

    private void uploadSignature() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("pdf.sign.signature.upload.title"));
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(
                        Messages.get("pdf.sign.signature.upload.filter"),
                        List.of("*.png", "*.jpg", "*.jpeg", "*.PNG", "*.JPG", "*.JPEG")));
        File chosen = chooser.showOpenDialog(owner);
        if (chosen != null) {
            viewModel.importSignature(chosen.toPath());
        }
    }

    /** Dipanggil kanvas saat user menempel TTD padahal belum ada: buka dialog gambar dulu. */
    private void askForSignature(Point2D position) {
        viewModel.warn(Messages.get("pdf.sign.signature.missing"));
        drawSignature();
    }
}
