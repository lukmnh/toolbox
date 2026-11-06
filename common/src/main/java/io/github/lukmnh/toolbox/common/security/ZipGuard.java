package io.github.lukmnh.toolbox.common.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/** Cegah zip bomb (mis. {@code .xlsx}) sebelum file dibuka library yang men-decompress isinya. */
public final class ZipGuard {

    private static final int BUFFER_SIZE = 8_192;

    private ZipGuard() {}

    /**
     * Hitung byte hasil decompress sungguhan (bukan ukuran yang diklaim header ZIP, yang bisa dipalsukan). Berhenti
     * menghitung begitu melewati {@code maxBytes}.
     *
     * @return total byte terhitung; nilai {@code > maxBytes} berarti isi ZIP terlalu besar
     * @throws ZipException bila file bukan ZIP valid atau jumlah entry melebihi {@code maxEntries}
     * @throws IOException bila file gagal dibaca
     */
    public static long uncompressedSize(Path file, long maxBytes, int maxEntries) throws IOException {
        long total = 0;
        int count = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (ZipFile zip = new ZipFile(file.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (++count > maxEntries) {
                    throw new ZipException("Too many ZIP entries");
                }
                try (InputStream content = zip.getInputStream(entry)) {
                    for (int read = content.read(buffer); read != -1; read = content.read(buffer)) {
                        total += read;
                        if (total > maxBytes) {
                            return total;
                        }
                    }
                }
            }
        }
        return total;
    }
}
