package io.github.lukmnh.toolbox.productivity.budget.domain;

import java.util.Objects;
import java.util.UUID;

/** Id internal entitas budget. Id stabil membuat rename kategori/daftar tidak perlu memperbarui referensi. */
final class Ids {

    static final int MAX_LENGTH = 64;

    private Ids() {}

    static String next() {
        return UUID.randomUUID().toString();
    }

    static String require(String id, String field) {
        Objects.requireNonNull(id, field);
        if (id.isBlank() || id.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return id;
    }
}
