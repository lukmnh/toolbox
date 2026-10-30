package io.github.lukmnh.toolbox.pdf.compress.infrastructure;

import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.pdf.compress.domain.CompressionPreset;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDICCBased;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Mengompres ulang satu gambar menjadi JPEG dan menggantinya langsung di stream aslinya, sehingga semua
 * halaman yang memakai gambar itu ikut terganti.
 *
 * <p>Aturan: gambar dengan alpha/SMask, image mask, 1-bit (CCITT/JBIG2) tidak disentuh. Hasil yang tidak
 * lebih kecil dibuang.
 */
final class ImageRecompressor {

    /** Gambar di atas target + toleransi ini baru di-downscale, supaya tidak resample untuk selisih kecil. */
    static final int DPI_TOLERANCE = 20;

    static final int MIN_FLATE_DIMENSION = 64;

    /** Flate → JPEG tanpa downscale hanya bila hemat minimal 20%, karena JPEG bersifat lossy. */
    static final double FLATE_MAX_SIZE_RATIO = 0.8;

    private static final int EIGHT_BITS = 8;
    private static final Set<COSName> UNTOUCHABLE_FILTERS = Set.of(COSName.CCITTFAX_DECODE, COSName.JBIG2_DECODE);
    private static final List<COSName> COPIED_KEYS =
            List.of(COSName.FILTER, COSName.WIDTH, COSName.HEIGHT, COSName.BITS_PER_COMPONENT, COSName.COLORSPACE);
    private static final List<COSName> OBSOLETE_KEYS = List.of(COSName.DECODE_PARMS, COSName.DECODE);

    private final PDDocument document;

    ImageRecompressor(PDDocument document) {
        this.document = document;
    }

    /** @return {@code true} bila gambar diganti */
    boolean recompress(PDImageXObject image, double effectiveDpi, CompressionPreset preset) throws IOException {
        if (!isEligible(image)) {
            return false;
        }
        Optional<PDImageXObject> replacement = createReplacement(image, effectiveDpi, preset);
        if (replacement.isEmpty()) {
            return false;
        }
        replaceStreamContent(image.getCOSObject(), replacement.get().getCOSObject());
        return true;
    }

    static boolean isEligible(PDImageXObject image) throws IOException {
        COSStream stream = image.getCOSObject();
        if (image.isStencil() || stream.containsKey(COSName.SMASK) || stream.containsKey(COSName.MASK)) {
            return false;
        }
        if (image.getBitsPerComponent() == 1) {
            return false;
        }
        // Dimensi dari dictionary (/Width /Height) dicek sebelum getImage(): gambar raksasa dibiarkan apa adanya
        // daripada di-decode dan menghabiskan heap (decompression bomb)
        if (Math.max(image.getWidth(), image.getHeight()) > SecurityLimits.IMAGE_MAX_DIMENSION) {
            return false;
        }
        List<COSName> filters = image.getStream().getFilters();
        return filters.stream().noneMatch(UNTOUCHABLE_FILTERS::contains);
    }

    private Optional<PDImageXObject> createReplacement(
            PDImageXObject image, double effectiveDpi, CompressionPreset preset) throws IOException {
        long originalLength = image.getCOSObject().getLength();
        if (effectiveDpi > preset.targetDpi() + DPI_TOLERANCE) {
            BufferedImage scaled = downscale(image.getImage(), preset.targetDpi() / effectiveDpi);
            PDImageXObject jpeg = JPEGFactory.createFromImage(document, scaled, preset.jpegQuality());
            return jpeg.getCOSObject().getLength() < originalLength ? Optional.of(jpeg) : Optional.empty();
        }
        if (!isLosslessCandidate(image)) {
            return Optional.empty();
        }
        PDImageXObject jpeg = JPEGFactory.createFromImage(document, image.getImage(), preset.jpegQuality());
        boolean worthIt = jpeg.getCOSObject().getLength() <= originalLength * FLATE_MAX_SIZE_RATIO;
        return worthIt ? Optional.of(jpeg) : Optional.empty();
    }

    /** Flate (atau tanpa filter), 8-bit RGB/Gray, minimal 64×64. */
    private static boolean isLosslessCandidate(PDImageXObject image) throws IOException {
        List<COSName> filters = image.getStream().getFilters();
        boolean flateOrRaw = filters.isEmpty() || filters.equals(List.of(COSName.FLATE_DECODE));
        return flateOrRaw
                && image.getBitsPerComponent() == EIGHT_BITS
                && isRgbOrGray(image.getColorSpace())
                && image.getWidth() >= MIN_FLATE_DIMENSION
                && image.getHeight() >= MIN_FLATE_DIMENSION;
    }

    private static boolean isRgbOrGray(PDColorSpace colorSpace) {
        return switch (colorSpace) {
            case PDDeviceRGB rgb -> true;
            case PDDeviceGray gray -> true;
            case PDICCBased icc -> icc.getNumberOfComponents() == 1 || icc.getNumberOfComponents() == 3;
            default -> false;
        };
    }

    static BufferedImage downscale(BufferedImage source, double factor) {
        int width = Math.max(1, (int) Math.round(source.getWidth() * factor));
        int height = Math.max(1, (int) Math.round(source.getHeight() * factor));
        boolean gray = source.getColorModel().getNumColorComponents() == 1;
        BufferedImage target =
                new BufferedImage(width, height, gray ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private static void replaceStreamContent(COSStream target, COSStream source) throws IOException {
        try (InputStream in = source.createRawInputStream();
                OutputStream out = target.createRawOutputStream()) {
            in.transferTo(out);
        }
        COPIED_KEYS.forEach(key -> target.setItem(key, source.getItem(key)));
        // Decode/DecodeParms milik encoding lama; getImage() sudah menerapkannya ke pixel baru
        OBSOLETE_KEYS.forEach(target::removeItem);
    }
}
