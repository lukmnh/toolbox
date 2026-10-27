package io.github.lukmnh.toolbox.pdf.compress.infrastructure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

/**
 * Menelusuri content stream untuk menemukan gambar yang benar-benar digambar dan resolusi efektifnya
 * (pixel gambar dibagi ukuran tampil dalam inch, dihitung dari CTM saat operator {@code Do}).
 */
final class ImageUsageCollector extends PDFStreamEngine {

    private static final double POINTS_PER_INCH = 72.0;

    /** Satu gambar unik (per identity {@link COSStream}) dan DPI tampil tertingginya. */
    static final class ImageUsage {
        private final PDImageXObject image;
        private double maxEffectiveDpi;

        private ImageUsage(PDImageXObject image) {
            this.image = image;
        }

        PDImageXObject image() {
            return image;
        }

        double maxEffectiveDpi() {
            return maxEffectiveDpi;
        }
    }

    // Identity: gambar yang sama dipakai di banyak halaman cukup diproses sekali
    private final Map<COSStream, ImageUsage> usages = new IdentityHashMap<>();
    private final List<ImageUsage> ordered = new ArrayList<>();

    ImageUsageCollector() {
        addOperator(new Concatenate(this));
        addOperator(new SetGraphicsStateParameters(this));
        addOperator(new Save(this));
        addOperator(new Restore(this));
        addOperator(new SetMatrix(this));
    }

    void collect(PDPage page) throws IOException {
        processPage(page);
    }

    List<ImageUsage> usages() {
        return List.copyOf(ordered);
    }

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        if (!OperatorName.DRAW_OBJECT.equals(operator.getName())) {
            super.processOperator(operator, operands);
            return;
        }
        if (operands.isEmpty() || !(operands.getFirst() instanceof COSName name)) {
            return;
        }
        PDXObject xObject = getResources().getXObject(name);
        if (xObject instanceof PDImageXObject image) {
            record(image);
        } else if (xObject instanceof PDFormXObject form) {
            // Gambar di dalam Form XObject ikut dihitung dengan CTM gabungan
            showForm(form);
        }
    }

    private void record(PDImageXObject image) {
        ImageUsage usage = usages.computeIfAbsent(image.getCOSObject(), key -> {
            ImageUsage created = new ImageUsage(image);
            ordered.add(created);
            return created;
        });
        usage.maxEffectiveDpi = Math.max(usage.maxEffectiveDpi, effectiveDpi(image));
    }

    /** Gambar dipetakan ke unit square oleh CTM; panjang vektor sumbu = ukuran tampil dalam point. */
    private double effectiveDpi(PDImageXObject image) {
        Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
        double widthPoints = Math.hypot(ctm.getValue(0, 0), ctm.getValue(0, 1));
        double heightPoints = Math.hypot(ctm.getValue(1, 0), ctm.getValue(1, 1));
        if (widthPoints <= 0 || heightPoints <= 0) {
            return 0;
        }
        double dpiX = image.getWidth() / (widthPoints / POINTS_PER_INCH);
        double dpiY = image.getHeight() / (heightPoints / POINTS_PER_INCH);
        return Math.max(dpiX, dpiY);
    }
}
