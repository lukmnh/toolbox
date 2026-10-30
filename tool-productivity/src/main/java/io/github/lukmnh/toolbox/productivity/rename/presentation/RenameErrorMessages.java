package io.github.lukmnh.toolbox.productivity.rename.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.productivity.rename.domain.CorruptHistoryException;
import io.github.lukmnh.toolbox.productivity.rename.domain.InvalidRuleException;
import io.github.lukmnh.toolbox.productivity.rename.domain.InvalidSelectionException;
import io.github.lukmnh.toolbox.productivity.rename.domain.PathOutsideFolderException;
import io.github.lukmnh.toolbox.productivity.rename.domain.PlanOutdatedException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameFailedException;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameIoException;
import io.github.lukmnh.toolbox.productivity.rename.domain.TooManyFilesException;
import io.github.lukmnh.toolbox.productivity.rename.domain.UndoUnavailableException;
import java.util.List;
import javafx.stage.Window;

/** Memetakan error domain Batch Rename ke pesan berbahasa Indonesia. */
final class RenameErrorMessages {

    private static final String PREFIX = "productivity.rename.error.";
    /** Daftar file gagal rollback bisa ribuan; dialog cukup menampilkan sebagian. */
    private static final int MAX_LISTED_FILES = 10;

    private RenameErrorMessages() {}

    static String describe(RenameException error) {
        return switch (error) {
            case TooManyFilesException e -> Messages.format(PREFIX + "tooMany", e.maxFiles());
            case InvalidSelectionException e ->
                Messages.format(PREFIX + "selection." + e.reason().name(), e.fileName());
            case InvalidRuleException e ->
                Messages.get(PREFIX + "rule." + e.problem().name());
            case RenameIoException e -> Messages.format(PREFIX + "io", e.fileName());
            case PathOutsideFolderException e -> Messages.format(PREFIX + "outside", e.fileName());
            case RenameFailedException e -> describeFailure(e);
            case PlanOutdatedException e -> Messages.get(PREFIX + "outdated");
            case UndoUnavailableException e ->
                Messages.format(PREFIX + "undo." + e.reason().name(), e.fileName());
            case CorruptHistoryException e -> Messages.get(PREFIX + "undo.CORRUPT_HISTORY");
        };
    }

    static void show(Window owner, Throwable error) {
        if (error instanceof RenameException renameError) {
            ErrorDialogs.show(owner, Messages.get(PREFIX + "header"), describe(renameError));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }

    private static String describeFailure(RenameFailedException error) {
        List<String> notRestored = error.notRestored();
        if (notRestored.isEmpty()) {
            return Messages.format(PREFIX + "failed.rolledBack", error.fileName());
        }
        String listed = String.join(", ", notRestored.subList(0, Math.min(MAX_LISTED_FILES, notRestored.size())));
        int more = Math.max(0, notRestored.size() - MAX_LISTED_FILES);
        return Messages.format(PREFIX + "failed.partial", error.fileName(), notRestored.size(), listed, more);
    }
}
