package io.github.lukmnh.toolbox.pdf.sign.application;

import io.github.lukmnh.toolbox.pdf.shared.domain.PreviewScale;
import io.github.lukmnh.toolbox.pdf.sign.domain.EditSession;
import java.awt.image.BufferedImage;

/** Render satu halaman untuk preview pada {@link PreviewScale#PREVIEW_DPI}. */
public final class RenderPageUseCase {

    public BufferedImage execute(EditSession session, int pageIndex) {
        if (pageIndex < 0 || pageIndex >= session.pageCount()) {
            throw new IllegalArgumentException("Halaman di luar jangkauan: " + pageIndex);
        }
        return session.render(pageIndex, PreviewScale.PREVIEW_DPI);
    }
}
