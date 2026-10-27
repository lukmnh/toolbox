package io.github.lukmnh.toolbox.pdf.shared.infrastructure;

import io.github.lukmnh.toolbox.pdf.shared.domain.FontSpec;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Memilih satu font TrueType yang tersedia di Windows atau Ubuntu, dipakai untuk preview dan PDF.
 * Bila tidak ada satu pun, fallback ke Helvetica bawaan PDF.
 */
public final class FontResolver {

    private static final List<FontSpec.FileFont> CANDIDATES = List.of(
            new FontSpec.FileFont(Path.of("C:/Windows/Fonts/arial.ttf"), "Arial"),
            new FontSpec.FileFont(
                    Path.of("/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"), "Liberation Sans"),
            new FontSpec.FileFont(Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"), "DejaVu Sans"));

    private final List<FontSpec.FileFont> candidates;
    private final Predicate<Path> fileExists;

    public FontResolver() {
        this(CANDIDATES, Files::isRegularFile);
    }

    FontResolver(List<FontSpec.FileFont> candidates, Predicate<Path> fileExists) {
        this.candidates = List.copyOf(candidates);
        this.fileExists = fileExists;
    }

    public FontSpec resolve() {
        return candidates.stream()
                .filter(candidate -> fileExists.test(candidate.file()))
                .<FontSpec>map(candidate -> candidate)
                .findFirst()
                .orElseGet(FontSpec.BuiltinHelvetica::new);
    }

    /** Font PDF untuk {@code spec}. TrueType di-embed sebagai subset (hanya glyph yang dipakai). */
    public PDFont load(PDDocument document, FontSpec spec) {
        return switch (spec) {
            case FontSpec.FileFont fileFont -> loadTrueType(document, fileFont.file());
            case FontSpec.BuiltinHelvetica helvetica -> new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        };
    }

    private static PDFont loadTrueType(PDDocument document, Path file) {
        try {
            return PDType0Font.load(document, file.toFile());
        } catch (IOException e) {
            throw new PdfIoException(file, e);
        }
    }
}
