package io.github.lukmnh.toolbox.pdf.shared.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Dialog pilih file PDF. */
public final class PdfFileChooser {

    public static final Set<String> PDF_EXTENSIONS = Set.of("pdf");

    private PdfFileChooser() {}

    public static Optional<Path> chooseOne(Window owner) {
        File file = chooser().showOpenDialog(owner);
        return Optional.ofNullable(file).map(File::toPath);
    }

    public static List<Path> chooseMany(Window owner) {
        List<File> files = chooser().showOpenMultipleDialog(owner);
        return files == null ? List.of() : files.stream().map(File::toPath).toList();
    }

    private static FileChooser chooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Messages.get("pdf.chooser.title"));
        chooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter(Messages.get("pdf.chooser.filter"), "*.pdf", "*.PDF"));
        return chooser;
    }
}
