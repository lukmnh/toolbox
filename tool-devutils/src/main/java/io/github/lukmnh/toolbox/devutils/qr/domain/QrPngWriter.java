package io.github.lukmnh.toolbox.devutils.qr.domain;

import java.nio.file.Path;

/** Port penulisan PNG (temp) sebelum di-download. */
public interface QrPngWriter {

    /** @throws QrExportException bila file gagal ditulis */
    void write(QrRaster raster, Path target);
}
