package io.github.lukmnh.toolbox.devutils.jwt.application;

import java.util.Objects;

/** Key yang dimasukkan user di card verifikasi. */
public sealed interface KeyInput {

    /**
     * Secret HMAC dari {@code PasswordField}. Array di-wipe oleh use case setelah dipakai (Security #7).
     *
     * @param base64 {@code true} bila secret ditulis dalam Base64/Base64URL (misalnya nilai {@code k} JWK)
     */
    record Secret(char[] chars, boolean base64) implements KeyInput {

        public Secret {
            Objects.requireNonNull(chars, "chars");
        }
    }

    /** PEM public key, sertifikat X.509 PEM, atau JWK JSON. */
    record PublicKeyText(String text) implements KeyInput {

        public PublicKeyText {
            Objects.requireNonNull(text, "text");
        }
    }
}
