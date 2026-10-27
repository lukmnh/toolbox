package io.github.lukmnh.toolbox.pdf.sign.presentation;

import io.github.lukmnh.toolbox.pdf.shared.domain.PreviewScale;
import io.github.lukmnh.toolbox.pdf.sign.domain.Overlay;
import java.awt.image.BufferedImage;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

/**
 * Model overlay yang bisa diubah di UI. Posisi dan ukuran dalam pixel preview; dikonversi ke point saat
 * export lewat {@link #toOverlay()}.
 */
abstract sealed class OverlayItem permits OverlayItem.TextItem, OverlayItem.SignatureItem {

    private final int pageIndex;
    private final DoubleProperty x;
    private final DoubleProperty y;

    OverlayItem(int pageIndex, double x, double y) {
        this.pageIndex = pageIndex;
        this.x = new SimpleDoubleProperty(x);
        this.y = new SimpleDoubleProperty(y);
    }

    int pageIndex() {
        return pageIndex;
    }

    DoubleProperty xProperty() {
        return x;
    }

    DoubleProperty yProperty() {
        return y;
    }

    void moveTo(double newX, double newY) {
        x.set(newX);
        y.set(newY);
    }

    abstract Overlay toOverlay();

    /** Teks; {@code y} adalah baseline baris pertama. */
    static final class TextItem extends OverlayItem {
        private final StringProperty text = new SimpleStringProperty("");
        private final DoubleProperty fontSize;

        TextItem(int pageIndex, double x, double baselineY, double fontSize) {
            super(pageIndex, x, baselineY);
            this.fontSize = new SimpleDoubleProperty(fontSize);
        }

        StringProperty textProperty() {
            return text;
        }

        /** Dalam point, sama seperti di PDF. */
        DoubleProperty fontSizeProperty() {
            return fontSize;
        }

        @Override
        Overlay toOverlay() {
            return new Overlay.Text(
                    pageIndex(),
                    PreviewScale.toPoints(xProperty().get()),
                    PreviewScale.toPoints(yProperty().get()),
                    text.get(),
                    fontSize.get());
        }
    }

    /** Tanda tangan; {@code (x, y)} adalah pojok kiri-atas. */
    static final class SignatureItem extends OverlayItem {
        private final DoubleProperty width;
        private final DoubleProperty height;
        private final BufferedImage image;
        private final Image previewImage;

        SignatureItem(int pageIndex, double x, double y, double width, BufferedImage image, Image previewImage) {
            super(pageIndex, x, y);
            this.width = new SimpleDoubleProperty(width);
            this.height = new SimpleDoubleProperty(width * image.getHeight() / image.getWidth());
            this.image = image;
            this.previewImage = previewImage;
        }

        DoubleProperty widthProperty() {
            return width;
        }

        DoubleProperty heightProperty() {
            return height;
        }

        Image previewImage() {
            return previewImage;
        }

        /** Ubah ukuran dengan aspect ratio tetap, berpusat di titik tengah lama. */
        void resize(double newWidth) {
            double ratio = height.get() / width.get();
            double centerX = xProperty().get() + width.get() / 2;
            double centerY = yProperty().get() + height.get() / 2;
            width.set(newWidth);
            height.set(newWidth * ratio);
            moveTo(centerX - newWidth / 2, centerY - height.get() / 2);
        }

        @Override
        Overlay toOverlay() {
            return new Overlay.Image(
                    pageIndex(),
                    PreviewScale.toPoints(xProperty().get()),
                    PreviewScale.toPoints(yProperty().get()),
                    PreviewScale.toPoints(width.get()),
                    PreviewScale.toPoints(height.get()),
                    image);
        }
    }
}
