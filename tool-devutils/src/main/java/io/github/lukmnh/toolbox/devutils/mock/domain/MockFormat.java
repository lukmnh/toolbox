package io.github.lukmnh.toolbox.devutils.mock.domain;

/** Format output mock. */
public enum MockFormat {
    JSON("json"),
    CSV("csv"),
    SQL("sql");

    private final String extension;

    MockFormat(String extension) {
        this.extension = extension;
    }

    /** Ekstensi file tanpa titik. */
    public String extension() {
        return extension;
    }
}
