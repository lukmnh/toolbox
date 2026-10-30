package io.github.lukmnh.toolbox.devutils.qr.presentation;

import io.github.lukmnh.toolbox.devutils.qr.application.ExportQrPngUseCase;
import io.github.lukmnh.toolbox.devutils.qr.application.GenerateQrUseCase;
import java.util.Objects;

/** Kumpulan use case QR Code Generator, dirakit di composition root. */
public record QrUseCases(GenerateQrUseCase generate, ExportQrPngUseCase exportPng) {

    public QrUseCases {
        Objects.requireNonNull(generate, "generate");
        Objects.requireNonNull(exportPng, "exportPng");
    }
}
