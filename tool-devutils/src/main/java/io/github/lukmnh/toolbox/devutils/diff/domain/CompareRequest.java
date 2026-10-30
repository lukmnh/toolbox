package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.Objects;

/**
 * @param fileName nama di header unified diff ({@code --- a/<nama>} / {@code +++ b/<nama>}); sama untuk kedua sisi
 *     supaya {@code git apply} memperlakukannya sebagai modifikasi file yang sama
 */
public record CompareRequest(String oldText, String newText, String fileName, DiffOptions options) {

    public CompareRequest {
        Objects.requireNonNull(oldText, "oldText");
        Objects.requireNonNull(newText, "newText");
        Objects.requireNonNull(options, "options");
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
    }
}
