package io.github.lukmnh.toolbox.pdf.sign.presentation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;

/**
 * Preview halaman + lapisan overlay. Koordinat lapisan overlay = pixel preview (144 DPI); zoom hanya men-scale
 * node, tidak merender ulang halaman.
 */
final class PageCanvas extends ScrollPane {

    static final double MIN_ZOOM = 0.1;
    static final double MAX_ZOOM = 4;
    private static final double ZOOM_STEP = 1.15;
    private static final double SIGNATURE_RESIZE_STEP = 1.1;
    private static final double FIT_MARGIN = 32;
    private static final String DELETE_CHARACTER = "\u007f";

    private final SignViewModel viewModel;
    private final Consumer<Point2D> onSignatureMissing;
    private final ImageView pageView = new ImageView();
    private final Pane overlayLayer = new Pane();
    private final Pane page = new Pane(pageView, overlayLayer);
    private final Scale scale = new Scale(1, 1, 0, 0);
    private final DoubleProperty zoom = new SimpleDoubleProperty(1);
    private final Map<OverlayItem, Node> nodes = new HashMap<>();

    private TextOverlayNode editingNode;
    private Point2D dragOffset;

    /** @param onSignatureMissing dipanggil saat user menempel TTD padahal belum punya TTD */
    PageCanvas(SignViewModel viewModel, Consumer<Point2D> onSignatureMissing) {
        this.viewModel = viewModel;
        this.onSignatureMissing = onSignatureMissing;
        getStyleClass().add("page-canvas");
        setPannable(false);
        page.getStyleClass().add("page-sheet");
        page.getTransforms().add(scale);
        scale.xProperty().bind(zoom);
        scale.yProperty().bind(zoom);
        // Group membungkus page supaya ukuran konten ScrollPane ikut ter-scale
        setContent(new Group(page));

        pageView.imageProperty().bind(viewModel.pageImageProperty());
        // Gambar HiDPI punya pixel lebih banyak; ditampilkan di ukuran preview supaya koordinat overlay tetap sama
        DoubleBinding pageWidth = Bindings.createDoubleBinding(this::imageWidth, viewModel.pageImageProperty());
        DoubleBinding pageHeight = Bindings.createDoubleBinding(this::imageHeight, viewModel.pageImageProperty());
        pageView.setSmooth(true);
        pageView.fitWidthProperty().bind(pageWidth);
        pageView.fitHeightProperty().bind(pageHeight);
        overlayLayer.prefWidthProperty().bind(pageWidth);
        overlayLayer.prefHeightProperty().bind(pageHeight);
        overlayLayer.setFocusTraversable(true);
        overlayLayer
                .cursorProperty()
                .bind(Bindings.createObjectBinding(
                        () -> viewModel.modeProperty().get() == EditMode.SELECT ? Cursor.DEFAULT : Cursor.CROSSHAIR,
                        viewModel.modeProperty()));

        overlayLayer.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onLayerPressed);
        overlayLayer.addEventHandler(KeyEvent.KEY_TYPED, this::onKeyTyped);
        overlayLayer.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        addEventFilter(ScrollEvent.SCROLL, this::onScroll);

        viewModel.overlays().addListener((ListChangeListener<OverlayItem>) change -> rebuildOverlays());
        viewModel.pageIndexProperty().addListener((observable, previous, current) -> rebuildOverlays());
        viewModel.fontFamilyProperty().addListener((observable, previous, current) -> rebuildOverlays());
    }

    DoubleProperty zoomProperty() {
        return zoom;
    }

    void zoomIn() {
        setZoom(zoom.get() * ZOOM_STEP);
    }

    void zoomOut() {
        setZoom(zoom.get() / ZOOM_STEP);
    }

    void fitWidth() {
        double width = imageWidth();
        if (width > 0 && getViewportBounds().getWidth() > 0) {
            setZoom((getViewportBounds().getWidth() - FIT_MARGIN) / width);
        }
    }

    private void setZoom(double value) {
        zoom.set(Math.clamp(value, MIN_ZOOM, MAX_ZOOM));
    }

    /** Lebar halaman dalam pixel preview (bukan pixel gambar). */
    private double imageWidth() {
        Image image = viewModel.pageImageProperty().get();
        return image == null ? 0 : image.getWidth() / viewModel.pagePixelScale();
    }

    private double imageHeight() {
        Image image = viewModel.pageImageProperty().get();
        return image == null ? 0 : image.getHeight() / viewModel.pagePixelScale();
    }

    // ---------- Overlay nodes ----------

    private void rebuildOverlays() {
        stopEditing();
        overlayLayer.getChildren().clear();
        nodes.clear();
        int currentPage = viewModel.pageIndexProperty().get();
        viewModel.overlays().stream()
                .filter(item -> item.pageIndex() == currentPage)
                .forEach(item -> overlayLayer.getChildren().add(createNode(item)));
    }

    private Node createNode(OverlayItem item) {
        var selected = viewModel.selectedProperty().isEqualTo(item);
        Node node = switch (item) {
            case OverlayItem.TextItem text ->
                new TextOverlayNode(text, viewModel.fontFamilyProperty().get(), selected);
            case OverlayItem.SignatureItem signature -> new SignatureOverlayNode(signature, selected);
        };
        node.setCursor(Cursor.MOVE);
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> onNodePressed(event, item, node));
        node.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> onNodeDragged(event, item));
        nodes.put(item, node);
        return node;
    }

    private void onNodePressed(MouseEvent event, OverlayItem item, Node node) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        overlayLayer.requestFocus();
        if (editingNode != null && editingNode != node) {
            stopEditing();
        }
        viewModel.select(item);
        Point2D pointer = toLayer(event);
        dragOffset = new Point2D(
                pointer.getX() - item.xProperty().get(),
                pointer.getY() - item.yProperty().get());
        if (event.getClickCount() == 2 && node instanceof TextOverlayNode textNode) {
            startEditing(textNode);
        }
        event.consume();
    }

    private void onNodeDragged(MouseEvent event, OverlayItem item) {
        if (dragOffset == null || event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        Point2D pointer = toLayer(event);
        viewModel.moveItem(item, pointer.getX() - dragOffset.getX(), pointer.getY() - dragOffset.getY());
        event.consume();
    }

    private Point2D toLayer(MouseEvent event) {
        return overlayLayer.sceneToLocal(event.getSceneX(), event.getSceneY());
    }

    // ---------- Klik di area kosong ----------

    private void onLayerPressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY
                || viewModel.pageImageProperty().get() == null) {
            return;
        }
        overlayLayer.requestFocus();
        stopEditing();
        switch (viewModel.modeProperty().get()) {
            case SELECT -> viewModel.selectedProperty().set(null);
            case TEXT -> {
                OverlayItem.TextItem item = viewModel.addText(event.getX(), event.getY());
                findNode(item)
                        .filter(TextOverlayNode.class::isInstance)
                        .map(TextOverlayNode.class::cast)
                        .ifPresent(this::startEditing);
            }
            case SIGNATURE -> placeSignature(event.getX(), event.getY());
        }
    }

    private void placeSignature(double x, double y) {
        if (viewModel.addSignature(x, y).isEmpty()) {
            onSignatureMissing.accept(new Point2D(x, y));
        }
    }

    private Optional<Node> findNode(OverlayItem item) {
        return Optional.ofNullable(nodes.get(item));
    }

    // ---------- Edit teks ----------

    private void startEditing(TextOverlayNode node) {
        stopEditing();
        editingNode = node;
        node.editingProperty().set(true);
        overlayLayer.requestFocus();
    }

    void stopEditing() {
        if (editingNode == null) {
            return;
        }
        TextOverlayNode finished = editingNode;
        editingNode = null;
        finished.editingProperty().set(false);
        viewModel.finishEditing(finished.item());
    }

    private void onKeyTyped(KeyEvent event) {
        if (editingNode == null || event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
            return;
        }
        String typed = event.getCharacter();
        // Karakter kontrol (Enter, Backspace, Esc, Tab) ditangani di onKeyPressed
        if (typed.isEmpty() || typed.charAt(0) < ' ' || DELETE_CHARACTER.equals(typed)) {
            return;
        }
        viewModel.appendText(editingNode.item(), typed);
        event.consume();
    }

    private void onKeyPressed(KeyEvent event) {
        if (editingNode != null) {
            handleEditingKey(event);
            return;
        }
        boolean deleteKey = event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE;
        if (deleteKey && viewModel.selectedProperty().get() != null) {
            viewModel.removeItem(viewModel.selectedProperty().get());
            event.consume();
        }
    }

    private void handleEditingKey(KeyEvent event) {
        switch (event.getCode()) {
            case BACK_SPACE -> viewModel.deleteLastCharacter(editingNode.item());
            case ENTER -> viewModel.appendText(editingNode.item(), "\n");
            case ESCAPE -> stopEditing();
            default -> {
                return;
            }
        }
        event.consume();
    }

    // ---------- Scroll ----------

    /** Ctrl+scroll: resize TTD terpilih; tanpa TTD terpilih = zoom. */
    private void onScroll(ScrollEvent event) {
        if (!event.isControlDown() || event.getDeltaY() == 0) {
            return;
        }
        boolean grow = event.getDeltaY() > 0;
        if (!viewModel.resizeSelectedSignature(grow ? SIGNATURE_RESIZE_STEP : 1 / SIGNATURE_RESIZE_STEP)) {
            if (grow) {
                zoomIn();
            } else {
                zoomOut();
            }
        }
        event.consume();
    }
}
