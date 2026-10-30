package io.github.lukmnh.toolbox.devutils.mock.domain;

import java.io.Serial;

/** Gagal menulis file output; hanya nama file yang disimpan (tanpa path home). */
public final class MockIoException extends MockToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public MockIoException(String fileName, Throwable cause) {
        super("Could not write " + fileName, cause);
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
