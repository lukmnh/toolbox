package io.github.lukmnh.toolbox.devutils.mock.domain;

import java.io.IOException;
import java.nio.file.Path;

/** Port tulis output ke file secara streaming; implementasi di infrastructure. */
public interface MockFiles {

    /** @throws MockIoException bila gagal ditulis */
    void write(Path target, MockContent content);

    @FunctionalInterface
    interface MockContent {
        void writeTo(Appendable out) throws IOException;
    }
}
