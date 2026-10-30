package io.github.lukmnh.toolbox.productivity.text.domain;

import java.io.Serial;

/** Teks dimuat utuh ke memori dan editor, jadi ukurannya dibatasi (Security Requirements #3). */
public final class TextFileTooLargeException extends TextToolException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final long sizeBytes;
    private final long maxBytes;

    public TextFileTooLargeException(String fileName, long sizeBytes, long maxBytes) {
        super("File teks terlalu besar: " + fileName, null);
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
