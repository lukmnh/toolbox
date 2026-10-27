package io.github.lukmnh.toolbox.pdf.sign.presentation;

import io.github.lukmnh.toolbox.pdf.shared.domain.PreviewScale;
import io.github.lukmnh.toolbox.pdf.sign.domain.EditDefaults;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Teks overlay. Setiap baris adalah {@link Text} terpisah dengan origin di baseline dan jarak baris
 * 1.2 × ukuran font, persis seperti saat di-burn ke PDF.
 */
final class TextOverlayNode extends Group {

    private static final double OUTLINE_PADDING = 4;
    private static final double CARET_WIDTH = 1.5;
    private static final double CARET_ASCENT = 0.8;

    private final OverlayItem.TextItem item;
    private final String fontFamily;
    private final Group lines = new Group();
    private final Rectangle outline = new Rectangle();
    private final Rectangle caret = new Rectangle();
    private final BooleanProperty editing = new SimpleBooleanProperty();

    TextOverlayNode(OverlayItem.TextItem item, String fontFamily, ObservableBooleanValue selected) {
        this.item = item;
        this.fontFamily = fontFamily;
        layoutXProperty().bind(item.xProperty());
        layoutYProperty().bind(item.yProperty());
        outline.getStyleClass().add("overlay-outline");
        outline.setFill(Color.TRANSPARENT);
        outline.visibleProperty().bind(editing.or(selected));
        caret.getStyleClass().add("overlay-caret");
        caret.visibleProperty().bind(editing);
        getChildren().addAll(outline, lines, caret);
        item.textProperty().addListener((observable, previous, current) -> refresh());
        item.fontSizeProperty().addListener((observable, previous, current) -> refresh());
        editing.addListener((observable, previous, current) -> {
            outline.getStyleClass().remove("overlay-outline-editing");
            if (current) {
                outline.getStyleClass().add("overlay-outline-editing");
            }
        });
        refresh();
    }

    OverlayItem.TextItem item() {
        return item;
    }

    BooleanProperty editingProperty() {
        return editing;
    }

    private void refresh() {
        double fontPixels = PreviewScale.toPixels(item.fontSizeProperty().get());
        double lineHeight = fontPixels * EditDefaults.LINE_HEIGHT_FACTOR;
        Font font = Font.font(fontFamily, fontPixels);
        String[] textLines = item.textProperty().get().split("\\R", -1);
        lines.getChildren().clear();
        for (int index = 0; index < textLines.length; index++) {
            Text line = new Text(0, index * lineHeight, textLines[index]);
            line.setFont(font);
            line.setTextOrigin(VPos.BASELINE);
            line.getStyleClass().add("overlay-text");
            lines.getChildren().add(line);
        }
        placeCaret(textLines.length - 1, lineHeight, fontPixels);
        placeOutline(fontPixels);
    }

    private void placeCaret(int lastLine, double lineHeight, double fontPixels) {
        Text last = (Text) lines.getChildren().get(lastLine);
        caret.setX(last.getLayoutBounds().getWidth() + 1);
        caret.setY(lastLine * lineHeight - fontPixels * CARET_ASCENT);
        caret.setWidth(CARET_WIDTH);
        caret.setHeight(fontPixels);
    }

    private void placeOutline(double fontPixels) {
        Bounds bounds = lines.getLayoutBounds();
        // Teks kosong tetap punya area klik seukuran satu huruf
        double width = Math.max(bounds.getWidth(), fontPixels / 2);
        double minY = Math.min(bounds.getMinY(), -fontPixels * CARET_ASCENT);
        double height = Math.max(bounds.getMaxY(), 0) - minY;
        outline.setX(-OUTLINE_PADDING);
        outline.setY(minY - OUTLINE_PADDING);
        outline.setWidth(width + 2 * OUTLINE_PADDING);
        outline.setHeight(Math.max(height, fontPixels) + 2 * OUTLINE_PADDING);
    }
}
