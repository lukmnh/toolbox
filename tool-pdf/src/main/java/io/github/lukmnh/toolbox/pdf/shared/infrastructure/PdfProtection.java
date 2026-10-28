package io.github.lukmnh.toolbox.pdf.shared.infrastructure;

import io.github.lukmnh.toolbox.pdf.shared.domain.PdfSource;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

/**
 * Proteksi password PDF sumber, untuk di-apply ulang ke file hasil (Security Requirements #2).
 *
 * <p>Owner password asli hanya diketahui bila user membuka dokumen dengan owner password. Selain itu owner
 * password diganti nilai acak, sehingga pembatasan permission (cetak, salin, edit) tetap berlaku untuk siapa pun
 * yang membuka hasil dengan user password.
 */
public record PdfProtection(String userPassword, String ownerPassword, int permissions) {

    /** Hasil selalu AES-256: password dan permission sama, algoritma tidak pernah lebih lemah dari sumber. */
    private static final int KEY_LENGTH = 256;

    private static final int RANDOM_OWNER_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Wajib dipanggil tepat setelah load, sebelum dokumen disimpan atau di-{@code setAllSecurityToBeRemoved}. */
    static Optional<PdfProtection> of(PDDocument document, PdfSource source) {
        if (!document.isEncrypted()) {
            return Optional.empty();
        }
        boolean openedAsOwner = document.getCurrentAccessPermission().isOwnerPermission();
        String owner = openedAsOwner ? source.password() : randomPassword();
        return Optional.of(new PdfProtection(
                source.password(), owner, document.getEncryption().getPermissions()));
    }

    void applyTo(PDDocument target) throws IOException {
        StandardProtectionPolicy policy =
                new StandardProtectionPolicy(ownerPassword, userPassword, new AccessPermission(permissions));
        policy.setEncryptionKeyLength(KEY_LENGTH);
        target.protect(policy);
    }

    private static String randomPassword() {
        byte[] bytes = new byte[RANDOM_OWNER_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    @Override
    public String toString() {
        // Password tidak boleh ikut tercetak ke log
        return "PdfProtection[permissions=" + permissions + "]";
    }
}
