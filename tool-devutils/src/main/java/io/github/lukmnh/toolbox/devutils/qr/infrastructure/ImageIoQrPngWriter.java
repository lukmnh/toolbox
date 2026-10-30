package io.github.lukmnh.toolbox.devutils.qr.infrastructure;

import io.github.lukmnh.toolbox.devutils.qr.domain.QrExportException;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrPngWriter;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrRaster;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Tulis raster QR ke PNG lewat {@link ImageIO} (headless, tanpa Swing). */
public final class ImageIoQrPngWriter implements QrPngWriter {

    private static final String PNG = "png";

    @Override
    public void write(QrRaster raster, Path target) {
        String name = target.getFileName().toString();
        // Hitam-putih tanpa alpha: PNG lebih kecil dan latar tetap putih di viewer bertema gelap
        BufferedImage image = new BufferedImage(raster.side(), raster.side(), BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, raster.side(), raster.side(), raster.argb(), 0, raster.side());
        try {
            if (!ImageIO.write(image, PNG, target.toFile())) {
                throw new QrExportException(name, new IOException("PNG writer not available"));
            }
        } catch (IOException e) {
            throw new QrExportException(name, e);
        }
    }
}
