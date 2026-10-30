package io.github.lukmnh.toolbox.json.csv.presentation;

import io.github.lukmnh.toolbox.json.csv.domain.SplitMethod;

/** Pilihan cara split di editor; teks aturan dipakai ulang saat user berpindah cara. */
enum SplitMode {
    SEPARATOR,
    POSITIONS,
    REGEX;

    static SplitMode of(SplitMethod method) {
        return switch (method) {
            case SplitMethod.BySeparator ignored -> SEPARATOR;
            case SplitMethod.ByPositions ignored -> POSITIONS;
            case SplitMethod.ByRegex ignored -> REGEX;
        };
    }

    static String ruleText(SplitMethod method) {
        return switch (method) {
            case SplitMethod.BySeparator separator -> separator.separator();
            case SplitMethod.ByPositions positions -> positions.positions();
            case SplitMethod.ByRegex regex -> regex.pattern();
        };
    }

    SplitMethod toMethod(String rule) {
        return switch (this) {
            case SEPARATOR -> new SplitMethod.BySeparator(rule);
            case POSITIONS -> new SplitMethod.ByPositions(rule);
            case REGEX -> new SplitMethod.ByRegex(rule);
        };
    }
}
