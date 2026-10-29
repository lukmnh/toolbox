package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.io.Serial;

public final class TestTextTooLargeException extends RegexToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final long sizeBytes;
    private final long maxBytes;

    public TestTextTooLargeException(String fileName, long sizeBytes, long maxBytes) {
        super("Teks uji terlalu besar: " + fileName, null);
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.maxBytes = maxBytes;
    }

    public String fileName() {
        return fileName;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public long maxBytes() {
        return maxBytes;
    }
}
