package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import io.github.lukmnh.toolbox.common.report.PdfReport;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetFileTooLargeException;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetImporter;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetStorageException;
import io.github.lukmnh.toolbox.productivity.budget.domain.InvalidBudgetFileException;
import io.github.lukmnh.toolbox.productivity.budget.domain.InvalidBudgetFileException.Problem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

/**
 * Import file export Toolbox: {@code .xlsx} (sheet data tersembunyi) atau {@code .pdf} (lampiran JSON). Jenis file
 * ditentukan dari magic bytes, bukan ekstensi. Data yang ditemukan divalidasi oleh {@link BudgetJsonCodec}.
 */
public final class ExportedBudgetImporter implements BudgetImporter {

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ZIP_MAGIC = {'P', 'K', 3, 4};
    private static final String NO_LOCATION = "";

    private final BudgetJsonCodec codec;

    public ExportedBudgetImporter(BudgetJsonCodec codec) {
        this.codec = codec;
    }

    @Override
    public Budget read(Path source) {
        String fileName = source.getFileName().toString();
        byte[] data = switch (detect(source, fileName)) {
            case XLSX -> XlsxBudgetData.read(source, fileName);
            case PDF -> readPdf(source, fileName);
        };
        return codec.fromBytes(data, fileName);
    }

    private enum Format {
        XLSX,
        PDF
    }

    /** Ukuran dicek sebelum file dibuka parser mana pun. */
    private static Format detect(Path source, String fileName) {
        try {
            long size = Files.size(source);
            long max = SecurityLimits.BUDGET_EXPORT_MAX_FILE_BYTES;
            if (size > max) {
                throw new BudgetFileTooLargeException(fileName, size, max);
            }
            byte[] head;
            try (InputStream input = Files.newInputStream(source)) {
                head = input.readNBytes(PDF_MAGIC.length);
            }
            if (startsWith(head, PDF_MAGIC)) {
                return Format.PDF;
            }
            if (startsWith(head, ZIP_MAGIC)) {
                return Format.XLSX;
            }
            throw new InvalidBudgetFileException(fileName, Problem.NOT_TOOLBOX_EXPORT, NO_LOCATION, null);
        } catch (IOException e) {
            throw new BudgetStorageException(fileName, e);
        }
    }

    private static boolean startsWith(byte[] content, byte[] prefix) {
        return content.length >= prefix.length && Arrays.equals(content, 0, prefix.length, prefix, 0, prefix.length);
    }

    private static byte[] readPdf(Path source, String fileName) {
        long max = SecurityLimits.BUDGET_IMPORT_MAX_FILE_BYTES;
        Optional<byte[]> attachment;
        try {
            attachment = PdfReport.readAttachment(source, PdfBudgetReport.ATTACHMENT, max);
        } catch (PdfReport.AttachmentTooLargeException e) {
            // Ukuran pasti tidak diketahui (dibaca terhenti di batas), cukup tunjukkan bahwa batas terlampaui
            throw new BudgetFileTooLargeException(fileName, max + 1, max);
        } catch (IOException | RuntimeException e) {
            // PDFBox melempar beragam exception untuk PDF rusak/terenkripsi; semuanya berarti file tidak terbaca
            throw new InvalidBudgetFileException(fileName, Problem.UNREADABLE_FILE, NO_LOCATION, e);
        }
        return attachment.orElseThrow(
                () -> new InvalidBudgetFileException(fileName, Problem.NOT_TOOLBOX_EXPORT, NO_LOCATION, null));
    }
}
