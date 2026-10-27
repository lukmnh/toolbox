package io.github.lukmnh.toolbox.pdf.sign.presentation;

import io.github.lukmnh.toolbox.pdf.sign.application.ExportEditedPdfUseCase;
import io.github.lukmnh.toolbox.pdf.sign.application.ImportSignatureImageUseCase;
import io.github.lukmnh.toolbox.pdf.sign.application.LoadLastSignatureUseCase;
import io.github.lukmnh.toolbox.pdf.sign.application.OpenDocumentUseCase;
import io.github.lukmnh.toolbox.pdf.sign.application.RenderPageUseCase;
import io.github.lukmnh.toolbox.pdf.sign.application.SaveDrawnSignatureUseCase;
import io.github.lukmnh.toolbox.pdf.sign.application.UpdateFormFieldUseCase;
import java.util.Objects;

/** Kumpulan use case tool Isi & TTD, supaya konstruktor ViewModel tidak kepanjangan. */
public record SignUseCases(
        OpenDocumentUseCase open,
        RenderPageUseCase render,
        UpdateFormFieldUseCase updateField,
        ExportEditedPdfUseCase export,
        SaveDrawnSignatureUseCase saveDrawnSignature,
        ImportSignatureImageUseCase importSignature,
        LoadLastSignatureUseCase loadLastSignature) {

    public SignUseCases {
        Objects.requireNonNull(open, "open");
        Objects.requireNonNull(render, "render");
        Objects.requireNonNull(updateField, "updateField");
        Objects.requireNonNull(export, "export");
        Objects.requireNonNull(saveDrawnSignature, "saveDrawnSignature");
        Objects.requireNonNull(importSignature, "importSignature");
        Objects.requireNonNull(loadLastSignature, "loadLastSignature");
    }
}
