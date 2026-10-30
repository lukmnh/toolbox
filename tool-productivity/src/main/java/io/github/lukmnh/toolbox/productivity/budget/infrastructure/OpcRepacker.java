package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Menulis ulang ZIP hasil FastExcel menjadi ZIP standar.
 *
 * <p>FastExcel 0.20.2 (opczip 1.2.0) menulis data descriptor ZIP64 tanpa extra field ZIP64 di local header. Excel
 * membaca central directory sehingga tidak masalah, tetapi LibreOffice 24.x dan reader streaming menolak file itu
 * ("source file could not be loaded"). {@link ZipFile} membaca lewat central directory, lalu {@link ZipOutputStream}
 * menulis ulang dengan format yang diterima semua aplikasi.
 */
final class OpcRepacker {

    /** OPC menganjurkan {@code [Content_Types].xml} sebagai entry pertama; beberapa reader mengandalkannya. */
    private static final String CONTENT_TYPES = "[Content_Types].xml";

    private OpcRepacker() {}

    static void repack(Path source, Path target) throws IOException {
        try (ZipFile input = new ZipFile(source.toFile());
                OutputStream file = Files.newOutputStream(target);
                ZipOutputStream output = new ZipOutputStream(file)) {
            for (ZipEntry entry : orderedEntries(input)) {
                output.putNextEntry(new ZipEntry(entry.getName()));
                try (InputStream content = input.getInputStream(entry)) {
                    content.transferTo(output);
                }
                output.closeEntry();
            }
        }
    }

    private static List<? extends ZipEntry> orderedEntries(ZipFile input) {
        return Collections.list(input.entries()).stream()
                .sorted(Comparator.comparing(entry -> !entry.getName().equals(CONTENT_TYPES)))
                .toList();
    }
}
