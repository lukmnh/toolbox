package io.github.lukmnh.toolbox.devutils.hash.domain;

/** Algoritma hash yang ditampilkan sekaligus. Urutan enum = urutan tampil. */
public enum HashAlgorithm {
    MD5("MD5", "MD5", true),
    SHA_1("SHA-1", "SHA-1", true),
    SHA_256("SHA-256", "SHA-256", false),
    SHA_384("SHA-384", "SHA-384", false),
    SHA_512("SHA-512", "SHA-512", false),
    SHA3_256("SHA3-256", "SHA3-256", false),
    SHA3_512("SHA3-512", "SHA3-512", false);

    private final String label;
    private final String jcaName;
    private final boolean insecure;

    HashAlgorithm(String label, String jcaName, boolean insecure) {
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

    /** {@code true} untuk algoritma yang sudah rentan collision (MD5, SHA-1). */
    public boolean insecure() {
        return insecure;
    }
}
