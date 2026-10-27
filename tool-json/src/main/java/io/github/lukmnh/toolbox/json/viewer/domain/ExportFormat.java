package io.github.lukmnh.toolbox.json.viewer.domain;

/** Format download hasil view. */
public enum ExportFormat {
    JSON_ARRAY("json"),
    JSON_LINES("jsonl"),
    CSV("csv");

    private final String extension;

    ExportFormat(String extension) {
        this.extension = extension;
    }

    public String fileName(String baseName) {
        return baseName + "." + extension;
    }
}
