package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Membuat nama unik ({@code Rumah}, {@code Rumah (2)}, …) tanpa membedakan huruf besar/kecil. */
public final class BudgetNames {

    private static final int FIRST_SUFFIX = 2;

    private BudgetNames() {}

    public static String unique(String desired, Collection<String> existing) {
        Set<String> taken = existing.stream().map(BudgetNames::key).collect(Collectors.toSet());
        String base = desired.strip();
        if (!taken.contains(key(base))) {
            return base;
        }
        for (int counter = FIRST_SUFFIX; ; counter++) {
            String suffix = " (" + counter + ")";
            String candidate = truncate(base, Texts.MAX_NAME_LENGTH - suffix.length()) + suffix;
            if (!taken.contains(key(candidate))) {
                return candidate;
            }
        }
    }

    private static String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength).strip();
    }

    private static String key(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
