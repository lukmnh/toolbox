package io.github.lukmnh.toolbox.pdf.sign.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.pdf.sign.domain.EditSession;
import io.github.lukmnh.toolbox.pdf.sign.domain.Overlay;
import java.nio.file.Path;
import java.util.List;

/** Menulis salinan PDF dengan teks dan tanda tangan di-burn ke halaman. */
public final class ExportEditedPdfUseCase {

    public Path execute(EditSession session, List<Overlay> overlays, Path output, CancellationToken cancellation) {
        cancellation.throwIfCancelled();
        List<Overlay> visible =
                overlays.stream().filter(ExportEditedPdfUseCase::hasContent).toList();
        for (Overlay overlay : visible) {
            if (overlay.pageIndex() >= session.pageCount()) {
                throw new IllegalArgumentException("Overlay di halaman yang tidak ada: " + overlay.pageIndex());
            }
        }
        session.export(visible, output);
        return output;
    }

    /** Teks kosong tidak ikut di-burn. */
    private static boolean hasContent(Overlay overlay) {
        return switch (overlay) {
            case Overlay.Text text -> !text.text().isBlank();
            case Overlay.Image image -> true;
        };
    }
}
