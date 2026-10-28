package io.github.lukmnh.toolbox.devutils.jwt.domain;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;

/** Material key untuk verifikasi. Dipisah per jenis supaya secret dan public key tidak pernah tertukar (#20). */
public sealed interface VerificationKey {

    /**
     * Secret HMAC. Array sengaja tidak di-copy supaya pemanggil bisa meng-{@link #wipe()} satu-satunya salinan
     * setelah verifikasi (Security Requirements #7).
     */
    record HmacSecret(byte[] bytes) implements VerificationKey {

        public HmacSecret {
            Objects.requireNonNull(bytes, "bytes");
        }

        public void wipe() {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    /** Public key RSA, EC, atau EdDSA. */
    record Public(PublicKey key) implements VerificationKey {

        public Public {
            Objects.requireNonNull(key, "key");
        }
    }
}
