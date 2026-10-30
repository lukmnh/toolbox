package io.github.lukmnh.toolbox.devutils.hash.infrastructure;

import io.github.lukmnh.toolbox.devutils.hash.domain.DigestResult;
import io.github.lukmnh.toolbox.devutils.hash.domain.HashAlgorithm;
import io.github.lukmnh.toolbox.devutils.hash.domain.HmacAlgorithm;
import io.github.lukmnh.toolbox.devutils.hash.domain.TextHasher;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Hash ({@code MessageDigest}) dan HMAC ({@code javax.crypto.Mac}) dari JDK. Stateless dan thread-safe. */
public final class JdkTextHasher implements TextHasher {

    @Override
    public DigestResult digest(HashAlgorithm algorithm, byte[] input) {
        try {
            byte[] digest = MessageDigest.getInstance(algorithm.jcaName()).digest(input);
            return new DigestResult(algorithm.label(), digest, algorithm.insecure());
        } catch (GeneralSecurityException e) {
            // Semua algoritma di enum wajib tersedia di JDK 21; kalau tidak, runtime-nya rusak
            throw new IllegalStateException("Algorithm " + algorithm.jcaName() + " is not available in the JDK", e);
        }
    }

    /** @throws IllegalArgumentException bila key kosong ({@link SecretKeySpec} menolak key kosong) */
    @Override
    public DigestResult hmac(HmacAlgorithm algorithm, byte[] key, byte[] input) {
        if (key.length == 0) {
            throw new IllegalArgumentException("HMAC secret key is empty");
        }
        try {
            Mac mac = Mac.getInstance(algorithm.jcaName());
            // Batasan JDK: SecretKeySpec & Mac menyalin key ke array internal yang tidak bisa di-wipe dari luar
            mac.init(new SecretKeySpec(key, algorithm.jcaName()));
            return new DigestResult(algorithm.label(), mac.doFinal(input), algorithm.insecure());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Algorithm " + algorithm.jcaName() + " is not available in the JDK", e);
        }
    }
}
