package io.github.lukmnh.toolbox.devutils.sql.domain;

/**
 * Satu contoh SQL di cheatsheet/konsep. Penjelasan di messages {@code devutils.sql.sheet.<key>} dan kapan
 * dipakai di {@code devutils.sql.sheet.<key>.when}.
 */
public record SqlSnippet(String group, String code, String key) {}
