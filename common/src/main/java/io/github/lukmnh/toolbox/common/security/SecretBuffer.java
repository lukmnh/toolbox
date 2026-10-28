package io.github.lukmnh.toolbox.common.security;

import java.util.Arrays;
import java.util.function.Consumer;

/** Memakai rahasia {@code char[]} (password) lalu menghapusnya dari memory (Security Requirements #7). */
public final class SecretBuffer {

    private SecretBuffer() {}

    /**
     * Konversi ke {@link String} hanya untuk API yang mewajibkannya (misalnya PDFBox). Array selalu di-wipe,
     * termasuk bila {@code consumer} melempar exception.
     */
    public static void consume(char[] secret, Consumer<String> consumer) {
        try {
            consumer.accept(new String(secret));
        } finally {
            Arrays.fill(secret, '\0');
        }
    }
}
