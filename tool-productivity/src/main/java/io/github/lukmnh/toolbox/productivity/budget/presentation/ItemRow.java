package io.github.lukmnh.toolbox.productivity.budget.presentation;

import io.github.lukmnh.toolbox.productivity.budget.domain.Category;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseItem;
import io.github.lukmnh.toolbox.productivity.budget.domain.ExpenseSheet;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Baris tabel item: item + nomor urut + kategori yang sudah di-resolve.
 *
 * @param category pilihan kategori item ({@link CategoryChoice#NONE} bila tanpa kategori)
 * @param color warna kategori {@code #RRGGBB}, kosong bila tanpa kategori
 */
record ItemRow(int number, ExpenseItem item, CategoryChoice category, String color) {

    static List<ItemRow> of(ExpenseSheet sheet) {
        List<ExpenseItem> items = sheet.items();
        return IntStream.range(0, items.size())
                .mapToObj(index -> row(index + 1, items.get(index), sheet))
                .toList();
    }

    private static ItemRow row(int number, ExpenseItem item, ExpenseSheet sheet) {
        return sheet.category(item.categoryId())
                .map(category -> new ItemRow(number, item, CategoryChoice.of(category), category.color()))
                .orElse(new ItemRow(number, item, CategoryChoice.NONE, ""));
    }

    String id() {
        return item.id();
    }

    BigDecimal monthly() {
        return item.monthlyCost();
    }

    /** Pilihan kolom Kategori di tabel. */
    record CategoryChoice(String id, String name) {

        static final CategoryChoice NONE = new CategoryChoice(ExpenseItem.NO_CATEGORY, "");

        static CategoryChoice of(Category category) {
            return new CategoryChoice(category.id(), category.name());
        }

        static List<CategoryChoice> all(ExpenseSheet sheet) {
            return Stream.concat(Stream.of(NONE), sheet.categories().stream().map(CategoryChoice::of))
                    .toList();
        }
    }
}
