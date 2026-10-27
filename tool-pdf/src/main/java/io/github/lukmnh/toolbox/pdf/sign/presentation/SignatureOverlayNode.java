package io.github.lukmnh.toolbox.pdf.sign.presentation;

import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** Tanda tangan di kanvas, dengan outline putus-putus saat terpilih. */
final class SignatureOverlayNode extends Group {

    private final OverlayItem.SignatureItem item;

    SignatureOverlayNode(OverlayItem.SignatureItem item, ObservableBooleanValue selected) {
        this.item = item;
        layoutXProperty().bind(item.xProperty());
        layoutYProperty().bind(item.yProperty());

        ImageView image = new ImageView(item.previewImage());
        image.setPreserveRatio(false);
        image.setSmooth(true);
        image.fitWidthProperty().bind(item.widthProperty());
        image.fitHeightProperty().bind(item.heightProperty());

        Rectangle outline = new Rectangle();
        outline.getStyleClass().add("overlay-outline");
        outline.setFill(Color.TRANSPARENT);
        outline.widthProperty().bind(item.widthProperty());
        outline.heightProperty().bind(item.heightProperty());
        outline.visibleProperty().bind(selected);
        // Outline tidak boleh menghalangi area klik gambar
        outline.setMouseTransparent(true);

        getChildren().addAll(image, outline);
    }

    OverlayItem.SignatureItem item() {
        return item;
    }
}
