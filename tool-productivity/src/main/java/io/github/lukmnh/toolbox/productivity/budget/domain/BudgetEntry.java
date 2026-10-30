package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Budget di daftar kiri.
 *
 * @param id identitas file di penyimpanan (bukan nama tampilan, karena nama boleh berisi karakter apa pun)
 */
public record BudgetEntry(String id, String name, Instant modified) {

    public BudgetEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(modified, "modified");
    }
}
