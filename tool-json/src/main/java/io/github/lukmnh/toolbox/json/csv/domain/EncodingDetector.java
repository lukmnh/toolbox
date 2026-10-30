package io.github.lukmnh.toolbox.json.csv.domain;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Deteksi encoding dari sampel awal file: BOM UTF-8, UTF-8 valid, atau fallback Windows-1252. */
public final class EncodingDetector {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private EncodingDetector() {}

    /** @param truncated sampel terpotong, jadi karakter multi-byte di ujung boleh belum lengkap */
    public static CsvEncoding detect(byte[] sample, boolean truncated) {
        if (startsWithBom(sample)) {
            return CsvEncoding.UTF_8_BOM;
        }
        return isValidUtf8(sample, truncated) ? CsvEncoding.UTF_8 : CsvEncoding.WINDOWS_1252;
    }

    /**
     * Teks sampel untuk deteksi pemisah dan header: BOM dibuang, dan bila terpotong, baris terakhir yang belum
     * lengkap ikut dibuang supaya tidak terhitung sebagai record dengan kolom kurang.
     */
    public static String decodeSample(byte[] sample, CsvEncoding encoding, boolean truncated) {
        int offset = encoding.isUtf8() && startsWithBom(sample) ? UTF8_BOM.length : 0;
        String text = new String(sample, offset, sample.length - offset, encoding.charset());
        if (!truncated) {
            return text;
        }
        int lastNewline = text.lastIndexOf('\n');
        return lastNewline > 0 ? text.substring(0, lastNewline) : text;
    }

    public static boolean startsWithBom(byte[] bytes) {
        if (bytes.length < UTF8_BOM.length) {
            return false;
        }
        for (int index = 0; index < UTF8_BOM.length; index++) {
            if (bytes[index] != UTF8_BOM[index]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidUtf8(byte[] sample, boolean truncated) {
        CharsetDecoder decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer output = CharBuffer.allocate(sample.length);
        // endOfInput=false untuk sampel terpotong: byte multi-byte yang belum lengkap di ujung tidak dianggap rusak
        CoderResult result = decoder.decode(ByteBuffer.wrap(sample), output, !truncated);
        if (result.isError()) {
            return false;
        }
        return truncated || !decoder.flush(output).isError();
    }
}
