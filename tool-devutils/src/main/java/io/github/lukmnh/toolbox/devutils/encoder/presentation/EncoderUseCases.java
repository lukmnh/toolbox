package io.github.lukmnh.toolbox.devutils.encoder.presentation;

import io.github.lukmnh.toolbox.devutils.encoder.application.DecodeToFileUseCase;
import io.github.lukmnh.toolbox.devutils.encoder.application.EncodeFileUseCase;
import io.github.lukmnh.toolbox.devutils.encoder.application.ReadFileUseCase;
import java.util.Objects;

/** Use case mode File, dikelompokkan supaya constructor tool dan ViewModel tetap ringkas. */
public record EncoderUseCases(EncodeFileUseCase encode, DecodeToFileUseCase decode, ReadFileUseCase read) {

    public EncoderUseCases {
        Objects.requireNonNull(encode, "encode");
        Objects.requireNonNull(decode, "decode");
        Objects.requireNonNull(read, "read");
    }
}
