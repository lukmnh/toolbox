package io.github.lukmnh.toolbox.devutils.encoder.infrastructure;

import io.github.lukmnh.toolbox.devutils.encoder.domain.Base64Alphabet;
import io.github.lukmnh.toolbox.devutils.encoder.domain.DataUri;
import io.github.lukmnh.toolbox.devutils.encoder.domain.InvalidBase64Exception;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Menyaring teks Base64 sebelum masuk decoder JDK: whitespace, prefix data URI, dan padding dibuang, alfabet URL-safe
 * diubah ke standar. Decoder JDK yang di-{@code wrap} tidak melaporkan posisi error, jadi validasi alfabet dilakukan di
 * sini dengan posisi byte terhadap file asli.
 */
final class Base64CleaningInputStream extends InputStream {

    /** Header data URI lebih panjang dari ini dianggap bukan data URI (lalu gagal sebagai karakter invalid). */
    private static final int PREFIX_SCAN_LIMIT = 4096;

    private static final int QUANTUM = 4;
    private static final int MAX_PADDING = 2;

    private final BufferedInputStream source;
    private boolean prefixChecked;
    private boolean endReached;
    private long position;
    private long dataCount;
    private long lastDataPosition = -1;
    private int padding;

    Base64CleaningInputStream(InputStream source) {
        this.source = new BufferedInputStream(source, PREFIX_SCAN_LIMIT);
    }

    /** Jumlah byte mentah yang sudah dibaca dari sumber, untuk progress. */
    long position() {
        return position;
    }

    /** Sumber habis dengan sisa 1 karakter data: tidak cukup untuk satu byte. */
    boolean endsWithDanglingCharacter() {
        return endReached && dataCount % QUANTUM == 1;
    }

    long lastDataPosition() {
        return lastDataPosition;
    }

    @Override
    public int read() throws IOException {
        byte[] single = new byte[1];
        int count = read(single, 0, 1);
        return count < 0 ? -1 : single[0] & 0xFF;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, buffer.length);
        if (length == 0) {
            return 0;
        }
        skipDataUriPrefix();
        while (true) {
            int count = source.read(buffer, offset, length);
            if (count < 0) {
                endReached = true;
                return -1;
            }
            int kept = clean(buffer, offset, count);
            if (kept > 0) {
                return kept;
            }
        }
    }

    /** Filter in-place: byte yang lolos digeser ke depan. */
    private int clean(byte[] buffer, int offset, int count) {
        int write = offset;
        for (int read = offset; read < offset + count; read++) {
            int character = buffer[read] & 0xFF;
            long current = position++;
            if (Base64Alphabet.isWhitespace(character)) {
                continue;
            }
            if (character == Base64Alphabet.PADDING) {
                padding++;
                requireValid(
                        padding <= MAX_PADDING, InvalidBase64Exception.Reason.MISPLACED_PADDING, current, character);
                continue;
            }
            requireValid(padding == 0, InvalidBase64Exception.Reason.MISPLACED_PADDING, current, character);
            int normalized = Base64Alphabet.normalize(character);
            requireValid(normalized >= 0, InvalidBase64Exception.Reason.INVALID_CHARACTER, current, character);
            buffer[write++] = (byte) normalized;
            dataCount++;
            lastDataPosition = current;
        }
        return write - offset;
    }

    private static void requireValid(
            boolean valid, InvalidBase64Exception.Reason reason, long position, int character) {
        if (!valid) {
            throw new InvalidBase64Exception(reason, position, character);
        }
    }

    private void skipDataUriPrefix() throws IOException {
        if (prefixChecked) {
            return;
        }
        prefixChecked = true;
        source.mark(PREFIX_SCAN_LIMIT);
        byte[] head = source.readNBytes(PREFIX_SCAN_LIMIT);
        source.reset();
        // ISO-8859-1 memetakan byte 1:1 ke char, jadi indeks char = offset byte
        int payloadStart = DataUri.payloadStart(new String(head, StandardCharsets.ISO_8859_1));
        if (payloadStart > 0) {
            source.skipNBytes(payloadStart);
            position += payloadStart;
        }
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}
