package io.github.lukmnh.toolbox.devutils.qr.domain;

/** Level error correction QR (ISO/IEC 18004). Makin tinggi makin tahan rusak, tapi kapasitas makin kecil. */
public enum QrErrorCorrection {
    L(7, 2953),
    M(15, 2331),
    Q(25, 1663),
    H(30, 1273);

    private final int recoveryPercent;
    private final int maxBytes;

    QrErrorCorrection(int recoveryPercent, int maxBytes) {
        this.recoveryPercent = recoveryPercent;
        this.maxBytes = maxBytes;
    }

    /** Perkiraan persentase codeword rusak yang masih bisa dipulihkan. */
    public int recoveryPercent() {
        return recoveryPercent;
    }

    /** Kapasitas mode byte di versi 40; teks alfanumerik murni bisa sedikit lebih banyak. */
    public int maxBytes() {
        return maxBytes;
    }
}
