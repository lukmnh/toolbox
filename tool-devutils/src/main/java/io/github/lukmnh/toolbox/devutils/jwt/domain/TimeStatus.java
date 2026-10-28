package io.github.lukmnh.toolbox.devutils.jwt.domain;

/** Status waktu token berdasarkan {@code exp} dan {@code nbf}. */
public enum TimeStatus {
    ACTIVE,
    EXPIRED,
    NOT_YET_VALID,
    NO_TIME_CLAIMS
}
