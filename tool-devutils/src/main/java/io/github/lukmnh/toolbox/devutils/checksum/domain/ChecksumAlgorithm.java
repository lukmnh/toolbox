package io.github.lukmnh.toolbox.devutils.checksum.domain;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/** Algoritma checksum file. Urutan enum = urutan tampil; SHA-256 default. */
public enum ChecksumAlgorithm {
    SHA_256("SHA-256", "SHA-256", 64, "SHA256SUMS"),
    SHA_1("SHA-1", "SHA-1", 40, "SHA1SUMS"),
    SHA_512("SHA-512", "SHA-512", 128, "SHA512SUMS"),
    MD5("MD5", "MD5", 32, "MD5SUMS"),
    /** Bukan MessageDigest JCA; dihitung lewat {@code java.util.zip.CRC32}. */
    CRC32("CRC32", "", 8, "CRC32SUMS");

    public static final ChecksumAlgorithm DEFAULT = SHA_256;

    private final String label;
    private final String jcaName;
    private final int hexLength;
    private final String sumsFileName;

    ChecksumAlgorithm(String label, String jcaName, int hexLength, String sumsFileName) {
        this.label = label;
        this.jcaName = jcaName;
        this.hexLength = hexLength;
        this.sumsFileName = sumsFileName;
    }

    public String label() {
        return label;
    }

    /** Nama untuk {@code MessageDigest.getInstance}; kosong untuk CRC32. */
    public String jcaName() {
        return jcaName;
    }

    public int hexLength() {
        return hexLength;
    }

    /** Nama file daftar checksum, kompatibel dengan {@code sha256sum -c}. */
    public String sumsFileName() {
        return sumsFileName;
    }

    /** Tebak algoritma dari panjang checksum hex (8=CRC32, 32=MD5, 40=SHA-1, 64=SHA-256, 128=SHA-512). */
    public static Optional<ChecksumAlgorithm> detect(String checksum) {
        String normalized = normalize(checksum);
        if (!isHex(normalized)) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(algorithm -> algorithm.hexLength == normalized.length())
                .findFirst();
    }

    /** Trim + huruf kecil, supaya perbandingan checksum case-insensitive. */
    public static String normalize(String checksum) {
        return checksum.strip().toLowerCase(Locale.ROOT);
    }

    public static boolean isHex(String text) {
        return !text.isEmpty() && text.chars().allMatch(HexFormat::isHexDigit);
    }
}
