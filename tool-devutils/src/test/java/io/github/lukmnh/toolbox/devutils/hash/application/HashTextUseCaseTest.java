package io.github.lukmnh.toolbox.devutils.hash.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.devutils.hash.domain.DigestEncoding;
import io.github.lukmnh.toolbox.devutils.hash.domain.DigestResult;
import io.github.lukmnh.toolbox.devutils.hash.domain.HashAlgorithm;
import io.github.lukmnh.toolbox.devutils.hash.infrastructure.JdkTextHasher;
import java.util.List;
import org.junit.jupiter.api.Test;

class HashTextUseCaseTest {

    private final HashTextUseCase useCase = new HashTextUseCase(new JdkTextHasher());

    @Test
    void returnsAllAlgorithmsInDisplayOrder() {
        List<DigestResult> results = useCase.execute("abc", CancellationToken.none());

        assertEquals(HashAlgorithm.values().length, results.size());
        for (int index = 0; index < results.size(); index++) {
            assertEquals(
                    HashAlgorithm.values()[index].label(), results.get(index).algorithm());
        }
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                results.get(HashAlgorithm.SHA_256.ordinal()).encoded(DigestEncoding.HEX_LOWER));
    }

    @Test
    void textIsHashedAsUtf8() {
        List<DigestResult> results = useCase.execute("é", CancellationToken.none());

        // "é" harus di-hash sebagai byte UTF-8 c3 a9, bukan ISO-8859-1 e9
        assertEquals(
                new JdkTextHasher()
                        .digest(HashAlgorithm.MD5, new byte[] {(byte) 0xc3, (byte) 0xa9})
                        .encoded(DigestEncoding.HEX_LOWER),
                results.getFirst().encoded(DigestEncoding.HEX_LOWER));
    }

    @Test
    void cancelledRunStops() {
        CancellationToken token = new CancellationToken();
        token.cancel();

        assertThrows(TaskCancelledException.class, () -> useCase.execute("abc", token));
    }
}
