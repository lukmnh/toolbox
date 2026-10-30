package io.github.lukmnh.toolbox.common.tool;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.Node;

/**
 * Proses yang tetap jalan saat user pindah tool (saat ini: Ambient Sound). Didaftarkan lewat
 * {@link ToolProvider#backgroundServices()}; shell hanya mengenal interface ini.
 *
 * <p>Thread milik service wajib daemon atau dihentikan di {@link #stop()}, dan exception di thread tersebut ditangkap
 * service sendiri (Security Requirements #22).
 */
public interface BackgroundService {

    String id();

    /** Idempotent. Tidak boleh ada thread yang dibuat sebelum method ini dipanggil. */
    void start();

    /** Idempotent. Dipanggil juga oleh shell saat aplikasi ditutup. */
    void stop();

    /** Berubah hanya di FX thread. */
    ReadOnlyBooleanProperty runningProperty();

    /** Kontrol mini di header shell, ditampilkan hanya selama {@link #runningProperty()} {@code true}. */
    Node statusIndicator(ToolContext context);
}
