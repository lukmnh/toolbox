package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.util.Objects;

/** Grup capture: nomor (mulai 1), nama (kosong bila tanpa nama), dan posisinya di pola. */
public record GroupInfo(int number, String name, Span span) {

    public GroupInfo {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(span, "span");
        if (number < 1) {
            throw new IllegalArgumentException("Group numbers start at 1");
        }
    }
}
