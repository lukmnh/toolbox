package io.github.lukmnh.toolbox.json.csv.domain;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Encoding file CSV. Windows-1252 hanya untuk input (fallback file lama dari Excel), output selalu UTF-8. */
public enum CsvEncoding {
    UTF_8(StandardCharsets.UTF_8, false),
    UTF_8_BOM(StandardCharsets.UTF_8, true),
    WINDOWS_1252(Charset.forName("windows-1252"), false);

    /** Pilihan encoding hasil. */
    public static final List<CsvEncoding> OUTPUT_CHOICES = List.of(UTF_8, UTF_8_BOM);

    private final Charset charset;
    private final boolean bom;

    CsvEncoding(Charset charset, boolean bom) {
        this.charset = charset;
        this.bom = bom;
    }

    public Charset charset() {
        return charset;
    }

    public boolean hasBom() {
        return bom;
    }

    /** UTF-8 dengan atau tanpa BOM; BOM di awal file selalu dibuang saat membaca. */
    public boolean isUtf8() {
        return charset.equals(StandardCharsets.UTF_8);
    }
}
