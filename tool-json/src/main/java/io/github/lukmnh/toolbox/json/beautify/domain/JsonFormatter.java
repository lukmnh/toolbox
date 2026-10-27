package io.github.lukmnh.toolbox.json.beautify.domain;

import io.github.lukmnh.toolbox.json.shared.domain.ScanProgress;
import java.nio.file.Path;

/** Port: format JSON secara streaming dan akses file hasilnya. */
public interface JsonFormatter {

    /** Ukuran file dalam byte. */
    long fileSize(Path file);

    /**
     * Parse {@code input} sampai habis dan tulis hasil sesuai {@code options}.
     *
     * @param output file tujuan; {@code null} untuk Validate (hanya parse)
     * @param progress byte yang sudah diproses dari total {@link JsonInput#sizeBytes()}
     * @throws io.github.lukmnh.toolbox.json.shared.domain.MalformedJsonException bila JSON tidak valid
     */
    FormatStats format(JsonInput input, FormatOptions options, Path output, ScanProgress progress);

    /** Baca maksimal {@code maxBytes} pertama sebagai UTF-8, tanpa memotong karakter. */
    TextPreview readText(Path file, int maxBytes);
}
