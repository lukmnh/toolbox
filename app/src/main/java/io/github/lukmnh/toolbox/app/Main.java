package io.github.lukmnh.toolbox.app;

import io.github.lukmnh.toolbox.common.logging.FileLogging;
import java.lang.System.Logger.Level;
import javafx.application.Application;

/**
 * Entry point. Sengaja bukan subclass {@link Application} supaya bisa dijalankan dari classpath biasa dan
 * dari jpackage tanpa konfigurasi module JavaFX tambahan.
 */
public final class Main {

    private static final System.Logger LOG = System.getLogger(Main.class.getName());

    private Main() {}

    public static void main(String[] args) {
        FileLogging.install();
        try {
            Application.launch(ToolboxApplication.class, args);
        } catch (RuntimeException | Error e) {
            // Exception dari init()/start() keluar lewat launch(), sebelum uncaught handler terpasang
            LOG.log(Level.ERROR, "Aplikasi crash saat startup", e);
            throw e;
        }
    }
}
