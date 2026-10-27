package io.github.lukmnh.toolbox.app;

import javafx.application.Application;

/**
 * Entry point. Sengaja bukan subclass {@link Application} supaya bisa dijalankan dari classpath biasa dan
 * dari jpackage tanpa konfigurasi module JavaFX tambahan.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        Application.launch(ToolboxApplication.class, args);
    }
}
