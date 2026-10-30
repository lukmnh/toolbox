package io.github.lukmnh.toolbox.json.beautify.domain;

/**
 * Statistik yang dihitung saat streaming.
 *
 * @param keyCount jumlah key di semua object
 * @param elementCount jumlah elemen di semua array
 */
public record FormatStats(long keyCount, long elementCount) {}
