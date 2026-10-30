package io.github.lukmnh.toolbox.devutils.hash.domain;

/** Algoritma HMAC ({@code javax.crypto.Mac}). Urutan enum = urutan tampil. */
public enum HmacAlgorithm {
    HMAC_MD5("HMAC-MD5", "HmacMD5", true),
    HMAC_SHA1("HMAC-SHA1", "HmacSHA1", true),
    HMAC_SHA256("HMAC-SHA256", "HmacSHA256", false),
    HMAC_SHA384("HMAC-SHA384", "HmacSHA384", false),
    HMAC_SHA512("HMAC-SHA512", "HmacSHA512", false);

    private final String label;
    private final String jcaName;
    private final boolean insecure;

    HmacAlgorithm(String label, String jcaName, boolean insecure) {
        this.label = label;
        this.jcaName = jcaName;
        this.insecure = insecure;
    }

    public String label() {
        return label;
    }

    public String jcaName() {
        return jcaName;
    }

    /** HMAC dengan hash lama masih dipakai sistem legacy, tapi tidak disarankan untuk sistem baru. */
    public boolean insecure() {
        return insecure;
    }
}
