package io.github.lukmnh.toolbox.json.viewer.domain;

import io.github.lukmnh.toolbox.json.shared.domain.ScanProgress;
import java.nio.file.Path;

/** Port: indexing dan akses file JSON besar tanpa memuat isinya ke memori. */
public interface JsonFiles {

    /**
     * Deteksi format lalu catat rentang byte tiap record.
     *
     * @throws io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException bila struktur JSON rusak
     * @throws io.github.lukmnh.toolbox.json.shared.domain.JsonIoException bila file gagal dibaca
     */
    RecordIndex index(Path file, ScanProgress progress);

    /** Buka file untuk membaca record. File tidak di-lock (aturan BR-2). */
    RecordFile open(Path file);
}
