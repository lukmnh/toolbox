package io.github.lukmnh.toolbox.json.csv.domain;

/** Alasan sebuah langkah pipeline tidak bisa dijalankan. */
public enum StepProblem {
    NO_SOURCE_COLUMN,
    MISSING_COLUMN,
    EMPTY_SEPARATOR,
    INVALID_POSITIONS,
    INVALID_REGEX,
    REGEX_WITHOUT_GROUP,
    TOO_FEW_MERGE_COLUMNS,
    DUPLICATE_MERGE_COLUMN,
    EMPTY_NEW_NAME,
    NO_COLUMNS_LEFT,
    DUPLICATE_COLUMN
}
