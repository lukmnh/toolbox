package io.github.lukmnh.toolbox.devutils.mock.application;

import io.github.lukmnh.toolbox.devutils.mock.domain.MockFormat;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockGenerator;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockRenderer;
import io.github.lukmnh.toolbox.devutils.mock.domain.MockSchema;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Preview: render maksimal {@link #PREVIEW_ROWS} baris ke string untuk area output. Cukup cepat untuk FX thread. */
public final class PreviewMockUseCase {

    public static final int PREVIEW_ROWS = 100;

    private final MockRenderer renderer;

    public PreviewMockUseCase(MockRenderer renderer) {
        this.renderer = renderer;
    }

    /** @throws io.github.lukmnh.toolbox.devutils.mock.domain.InvalidMockSchemaException bila schema tidak valid */
    public String execute(MockSchema schema, MockFormat format) {
        StringBuilder out = new StringBuilder();
        try {
            renderer.render(schema, format, MockGenerator.rows(schema), PREVIEW_ROWS, out, () -> {});
        } catch (IOException e) {
            // StringBuilder tidak pernah melempar IOException; dibungkus hanya karena kontrak Appendable
            throw new UncheckedIOException(e);
        }
        return out.toString();
    }
}
