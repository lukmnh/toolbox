package io.github.lukmnh.toolbox.productivity.ambient.domain;

import java.io.Serial;

/** Tidak ada perangkat audio yang bisa dibuka (tidak terpasang, dipakai eksklusif aplikasi lain, atau format ditolak). */
public final class AudioDeviceUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AudioDeviceUnavailableException(Throwable cause) {
        super("Perangkat audio tidak tersedia", cause);
    }
}
