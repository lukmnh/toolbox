package io.github.lukmnh.toolbox.devutils.hash.domain;

/** Algoritma key pair yang bisa di-generate. Urutan enum = urutan tampil. */
public enum KeyPairAlgorithm {
    RSA_2048("RSA 2048", "rsa-2048", Family.RSA, 2048),
    RSA_3072("RSA 3072", "rsa-3072", Family.RSA, 3072),
    RSA_4096("RSA 4096", "rsa-4096", Family.RSA, 4096),
    EC_P256("EC P-256", "ec-p256", Family.EC, 256),
    EC_P384("EC P-384", "ec-p384", Family.EC, 384),
    EC_P521("EC P-521", "ec-p521", Family.EC, 521),
    ED25519("Ed25519", "ed25519", Family.ED25519, 255);

    /** Keluarga algoritma JCA; menentukan cara inisialisasi {@code KeyPairGenerator}. */
    public enum Family {
        RSA("RSA"),
        EC("EC"),
        ED25519("Ed25519");

        private final String jcaName;

        Family(String jcaName) {
            this.jcaName = jcaName;
        }

        /** Nama untuk {@code KeyPairGenerator}/{@code KeyFactory}. */
        public String jcaName() {
            return jcaName;
        }
    }

    private final String label;
    private final String fileSlug;
    private final Family family;
    private final int bits;

    KeyPairAlgorithm(String label, String fileSlug, Family family, int bits) {
        this.label = label;
        this.fileSlug = fileSlug;
        this.family = family;
        this.bits = bits;
    }

    public String label() {
        return label;
    }

    /** Potongan nama file download, misalnya {@code rsa-2048}. */
    public String fileSlug() {
        return fileSlug;
    }

    public Family family() {
        return family;
    }

    /** Panjang modulus (RSA) atau ukuran kurva (EC/EdDSA). */
    public int bits() {
        return bits;
    }

    /** Nama kurva standar untuk {@code ECGenParameterSpec}; hanya valid untuk {@link Family#EC}. */
    public String curveName() {
        return switch (this) {
            case EC_P256 -> "secp256r1";
            case EC_P384 -> "secp384r1";
            case EC_P521 -> "secp521r1";
            default -> throw new IllegalStateException(label + " is not an EC curve");
        };
    }
}
