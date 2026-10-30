package io.github.lukmnh.toolbox.devutils.hash.domain;

import java.nio.file.Path;

/** Port penulisan file PEM (temp) sebelum di-download. */
public interface PemFiles {

    /** @throws KeyExportException bila file gagal ditulis */
    void write(Path target, String pem);
}
