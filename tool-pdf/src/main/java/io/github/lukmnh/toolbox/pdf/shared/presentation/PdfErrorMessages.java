package io.github.lukmnh.toolbox.pdf.shared.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.ByteSizeFormat;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.pdf.shared.domain.CorruptPdfException;
import io.github.lukmnh.toolbox.pdf.shared.domain.FormFieldException;
import io.github.lukmnh.toolbox.pdf.shared.domain.InvalidPageRangeException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PasswordRequiredException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfIoException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfLimitExceededException;
import io.github.lukmnh.toolbox.pdf.shared.domain.PdfToolException;
import io.github.lukmnh.toolbox.pdf.shared.domain.UnsupportedTextException;
import java.nio.file.Path;
import javafx.stage.Window;

/** Memetakan error domain PDF ke pesan berbahasa Indonesia yang menyebut nama file. */
public final class PdfErrorMessages {

    private PdfErrorMessages() {}

    public static void show(Window owner, Throwable error) {
        if (error instanceof PdfToolException pdfError) {
            ErrorDialogs.show(owner, header(pdfError), detail(pdfError));
            return;
        }
        ErrorDialogs.showUnexpected(owner, error);
    }

    static String header(PdfToolException error) {
        return switch (error) {
            case PasswordRequiredException e -> Messages.format("pdf.error.password", name(e.file()));
            case CorruptPdfException e -> Messages.format("pdf.error.corrupt", name(e.file()));
            case PdfIoException e -> Messages.format("pdf.error.io", name(e.file()));
            case InvalidPageRangeException e -> Messages.get("pdf.error.range.header");
            case FormFieldException e -> Messages.format("pdf.error.field", e.fieldName());
            case UnsupportedTextException e -> Messages.get("pdf.error.text.header");
            case PdfLimitExceededException e -> Messages.format("pdf.error.limit", name(e.file()));
        };
    }

    static String detail(PdfToolException error) {
        return switch (error) {
            case InvalidPageRangeException e -> rangeDetail(e);
            case UnsupportedTextException e -> Messages.format("pdf.error.text.detail", e.text());
            case PdfLimitExceededException e -> limitDetail(e);
            default -> ErrorDialogs.describe(error);
        };
    }

    private static String limitDetail(PdfLimitExceededException error) {
        return switch (error.limit()) {
            case FILE_SIZE ->
                Messages.format(
                        "pdf.error.limit.fileSize",
                        ByteSizeFormat.format(error.actual()),
                        ByteSizeFormat.format(error.maximum()));
            case PAGE_COUNT -> Messages.format("pdf.error.limit.pageCount", error.actual(), error.maximum());
            case RENDER_TIME -> Messages.format("pdf.error.limit.renderTime", error.actual(), error.maximum());
            case IMAGE_DIMENSION -> Messages.format("pdf.error.limit.imageDimension", error.actual(), error.maximum());
        };
    }

    private static String rangeDetail(InvalidPageRangeException error) {
        String key = switch (error.reason()) {
            case EMPTY -> "pdf.error.range.empty";
            case SYNTAX -> "pdf.error.range.syntax";
            case ZERO_OR_NEGATIVE -> "pdf.error.range.zero";
            case REVERSED -> "pdf.error.range.reversed";
            case OUT_OF_BOUNDS -> "pdf.error.range.outOfBounds";
        };
        return Messages.format(key, error.token(), error.pageCount());
    }

    private static String name(Path file) {
        return file.getFileName().toString();
    }
}
