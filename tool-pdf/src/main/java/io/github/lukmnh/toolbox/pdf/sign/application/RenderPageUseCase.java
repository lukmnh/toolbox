package io.github.lukmnh.toolbox.pdf.sign.application;

import io.github.lukmnh.toolbox.pdf.shared.domain.PreviewScale;
import io.github.lukmnh.toolbox.pdf.sign.domain.EditSession;
import java.awt.image.BufferedImage;

/** Render satu halaman untuk preview pada {@link PreviewScale#PREVIEW_DPI} × skala layar. */
public final class RenderPageUseCase {

    /** @param outputScale skala layar tempat preview tampil (1 = 100%), dibatasi {@link PreviewScale#MAX_OUTPUT_SCALE} */
    public BufferedImage execute(EditSession session, int pageIndex, double outputScale) {
        if (pageIndex < 0 || pageIndex >= session.pageCount()) {
            throw new IllegalArgumentException("Page out of range: " + pageIndex);
        }
        return session.render(pageIndex, PreviewScale.renderDpi(outputScale));
    }
}
