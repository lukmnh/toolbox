package io.github.lukmnh.toolbox.common.ui;

import java.util.List;

/** Notifikasi non-blocking (toast). Harus dipanggil dari FX thread. */
public interface Notifier {

    void info(String message);

    void warning(String message);

    void success(String message, List<NotificationAction> actions);
}
