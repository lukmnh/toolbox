package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Total rata-rata per bulan item aktif dalam satu kategori.
 *
 * @param categoryId id kategori, atau {@link ExpenseItem#NO_CATEGORY} untuk kelompok item tanpa kategori
 */
public record CategoryTotal(String categoryId, String name, String color, BigDecimal monthly) {

    public CategoryTotal {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(monthly, "monthly");
    }

    /** Item tanpa kategori dikumpulkan dalam satu kelompok; nama tampilannya ditentukan presentation. */
    public boolean uncategorized() {
        return categoryId.equals(ExpenseItem.NO_CATEGORY);
    }
}
