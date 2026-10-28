package io.github.lukmnh.toolbox.devutils.encoder.domain;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Tipe file hasil decode, dideteksi dari magic bytes (bukan dari nama file yang tidak ada). */
public enum FileType {
    PNG("png", true),
    JPEG("jpg", true),
    GIF("gif", true),
    /** JavaFX tidak bisa menampilkan WebP, jadi tanpa preview. */
    WEBP("webp", false),
    PDF("pdf", false),
    ZIP("zip", false),
    GZIP("gz", false),
    BINARY("bin", false);

    /** Byte terbanyak yang dibutuhkan deteksi (WebP: {@code RIFF????WEBP}). */
    public static final int HEADER_BYTES = 12;

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF_SIGNATURE = ascii("GIF8");
    private static final byte[] RIFF = ascii("RIFF");
    private static final byte[] WEBP_TAG = ascii("WEBP");
    private static final int WEBP_TAG_OFFSET = 8;
    private static final byte[] PDF_SIGNATURE = ascii("%PDF");
    private static final byte[] ZIP_SIGNATURE = {'P', 'K', 0x03, 0x04};
    private static final byte[] GZIP_SIGNATURE = {0x1F, (byte) 0x8B};

    private final String extension;
    private final boolean previewable;

    FileType(String extension, boolean previewable) {
        this.extension = extension;
        this.previewable = previewable;
    }

    public String extension() {
        return extension;
    }

    /** Gambar yang bisa ditampilkan JavaFX. */
    public boolean previewable() {
        return previewable;
    }

    /** @param header byte awal file (boleh lebih pendek dari {@link #HEADER_BYTES}) */
    public static FileType detect(byte[] header) {
        if (startsWith(header, PNG_SIGNATURE, 0)) {
            return PNG;
        }
        if (startsWith(header, JPEG_SIGNATURE, 0)) {
            return JPEG;
        }
        if (startsWith(header, GIF_SIGNATURE, 0)) {
            return GIF;
        }
        if (startsWith(header, RIFF, 0) && startsWith(header, WEBP_TAG, WEBP_TAG_OFFSET)) {
            return WEBP;
        }
        if (startsWith(header, PDF_SIGNATURE, 0)) {
            return PDF;
        }
        if (startsWith(header, ZIP_SIGNATURE, 0)) {
            return ZIP;
        }
        return startsWith(header, GZIP_SIGNATURE, 0) ? GZIP : BINARY;
    }

    private static boolean startsWith(byte[] data, byte[] signature, int offset) {
        if (data.length < offset + signature.length) {
            return false;
        }
        return Arrays.equals(data, offset, offset + signature.length, signature, 0, signature.length);
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }
}
