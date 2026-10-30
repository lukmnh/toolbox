package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Kategori item dalam satu daftar, misalnya "Basic" (cyan) atau "Make Up" (pink).
 *
 * @param color warna hex {@code #RRGGBB}; formatnya dikunci supaya aman dipakai di CSS inline dan Excel
 */
public record Category(String id, String name, String color) {

    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9A-F]{6}");

    public Category {
        id = Ids.require(id, "id kategori");
        name = Texts.requiredName(name, "nama kategori");
        Objects.requireNonNull(color, "warna kategori");
        color = color.toUpperCase(Locale.ROOT);
        if (!HEX_COLOR.matcher(color).matches()) {
            throw new IllegalArgumentException("warna kategori harus #RRGGBB");
        }
    }

    public static Category create(String name, String color) {
        return new Category(Ids.next(), name, color);
    }

    public Category withName(String newName) {
        return new Category(id, newName, color);
    }

    public Category withColor(String newColor) {
        return new Category(id, name, newColor);
    }
}
