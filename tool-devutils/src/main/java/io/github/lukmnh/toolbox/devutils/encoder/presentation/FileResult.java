package io.github.lukmnh.toolbox.devutils.encoder.presentation;

import io.github.lukmnh.toolbox.devutils.encoder.domain.DecodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.EncodedFile;
import io.github.lukmnh.toolbox.devutils.encoder.domain.ImageSize;
import io.github.lukmnh.toolbox.devutils.encoder.domain.TextPreview;
import java.nio.file.Path;

/** Hasil mode File yang siap ditampilkan. */
sealed interface FileResult {

    Path output();

    String suggestedName();

    record Encoded(EncodedFile file, TextPreview preview, String suggestedName) implements FileResult {

        @Override
        public Path output() {
            return file.output();
        }
    }

    record Decoded(DecodedFile file, ImagePreview image) implements FileResult {

        @Override
        public Path output() {
            return file.output();
        }

        @Override
        public String suggestedName() {
            return file.suggestedName();
        }
    }

    /** Gambar hanya di-decode oleh JavaFX bila dimensi dan ukurannya lolos batas (#3). */
    sealed interface ImagePreview {

        record None() implements ImagePreview {}

        record TooLarge(ImageSize size) implements ImagePreview {}

        record Allowed(ImageSize size) implements ImagePreview {}
    }
}
