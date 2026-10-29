package io.github.lukmnh.toolbox.devutils.regex.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * @param maxMatches match yang disimpan detailnya (sisanya hanya dihitung)
 * @param maxCount batas penghitungan total match
 */
public record ScanLimits(int maxMatches, int maxCount, Duration timeout) {

    public ScanLimits {
        Objects.requireNonNull(timeout, "timeout");
        if (maxMatches < 0 || maxCount < maxMatches) {
            throw new IllegalArgumentException("Batas scan tidak valid");
        }
    }
}
