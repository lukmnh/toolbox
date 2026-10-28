package io.github.lukmnh.toolbox.devutils.hash.domain;

/** Port ke provider kriptografi JDK ({@code javax.crypto} ada di luar batas domain). Harus thread-safe. */
public interface TextHasher {

    DigestResult digest(HashAlgorithm algorithm, byte[] input);

    /** @throws IllegalArgumentException bila {@code key} kosong */
    DigestResult hmac(HmacAlgorithm algorithm, byte[] key, byte[] input);
}
