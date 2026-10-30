package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.Objects;

/**
 * Hasil satu grup capture pada satu match.
 *
 * @param start indeks awal, atau {@link #NOT_MATCHED} bila grup tidak ikut cocok
 */
public record GroupMatch(int number, String name, int start, int end, String value) {

    public static final int NOT_MATCHED = -1;

    public GroupMatch {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
    }

    public boolean matched() {
        return start != NOT_MATCHED;
    }
}
