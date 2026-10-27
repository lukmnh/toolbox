package io.github.lukmnh.toolbox.json.beautify.domain;

/** Aksi JSON Beautify. Validate hanya memeriksa, tanpa menghasilkan file. */
public enum BeautifyAction {
    BEAUTIFY(".pretty.json"),
    MINIFY(".min.json"),
    VALIDATE("");

    private final String outputSuffix;

    BeautifyAction(String outputSuffix) {
        this.outputSuffix = outputSuffix;
    }

    public boolean writesOutput() {
        return this != VALIDATE;
    }

    /** Nama hasil: {@code <nama>.pretty.json} / {@code <nama>.min.json}. */
    public String outputName(String baseName) {
        if (!writesOutput()) {
            throw new IllegalStateException("Validate tidak menghasilkan file");
        }
        return baseName + outputSuffix;
    }
}
