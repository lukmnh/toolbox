package io.github.lukmnh.toolbox.common.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SecretBufferTest {

    @Test
    void passwordIsWipedAfterUse() {
        char[] password = "rahasia".toCharArray();
        AtomicReference<String> received = new AtomicReference<>();

        SecretBuffer.consume(password, received::set);

        assertEquals("rahasia", received.get());
        assertArrayEquals(new char[7], password);
    }

    @Test
    void passwordIsWipedEvenWhenConsumerFails() {
        char[] password = "rahasia".toCharArray();

        assertThrows(
                IllegalStateException.class,
                () -> SecretBuffer.consume(password, text -> {
                    throw new IllegalStateException("gagal");
                }));

        assertArrayEquals(new char[7], password);
    }
}
