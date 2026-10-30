package io.github.lukmnh.toolbox.productivity.rename.presentation;

import io.github.lukmnh.toolbox.productivity.rename.application.ExecuteRenameUseCase;
import io.github.lukmnh.toolbox.productivity.rename.application.FindLastRenameUseCase;
import io.github.lukmnh.toolbox.productivity.rename.application.LoadFilesUseCase;
import io.github.lukmnh.toolbox.productivity.rename.application.PreviewRenameUseCase;
import io.github.lukmnh.toolbox.productivity.rename.application.UndoLastRenameUseCase;
import java.util.Objects;

/** Kumpulan use case Batch Rename, supaya constructor tool dan ViewModel tidak terlalu panjang. */
public record RenameUseCases(
        LoadFilesUseCase load,
        PreviewRenameUseCase preview,
        ExecuteRenameUseCase execute,
        FindLastRenameUseCase findLast,
        UndoLastRenameUseCase undo) {

    public RenameUseCases {
        Objects.requireNonNull(load, "load");
        Objects.requireNonNull(preview, "preview");
        Objects.requireNonNull(execute, "execute");
        Objects.requireNonNull(findLast, "findLast");
        Objects.requireNonNull(undo, "undo");
    }
}
