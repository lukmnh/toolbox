package io.github.lukmnh.toolbox.devutils.sql.presentation;

/** Aksi yang menghasilkan output; menentukan label dan akhiran nama file download. */
enum SqlAction {
    FORMAT("devutils.sql.action.format", ".formatted.sql"),
    MINIFY("devutils.sql.action.minify", ".min.sql");

    private final String labelKey;
    private final String fileSuffix;

    SqlAction(String labelKey, String fileSuffix) {
        this.labelKey = labelKey;
        this.fileSuffix = fileSuffix;
    }

    String labelKey() {
        return labelKey;
    }

    String fileSuffix() {
        return fileSuffix;
    }
}
