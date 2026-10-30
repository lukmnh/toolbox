package io.github.lukmnh.toolbox.productivity.budget.infrastructure;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.lukmnh.toolbox.common.security.SecurityLimits;
import io.github.lukmnh.toolbox.productivity.budget.domain.Budget;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetFileTooLargeException;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetLimitExceededException;
import io.github.lukmnh.toolbox.productivity.budget.domain.BudgetStorageException;
import io.github.lukmnh.toolbox.productivity.budget.domain.InvalidBudgetFileException;
import io.github.lukmnh.toolbox.productivity.budget.domain.InvalidBudgetFileException.Problem;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Baca/tulis file JSON budget dengan Jackson databind. Dipakai penyimpanan dan export/import, sehingga file autosave
 * dan file export memakai skema yang sama. Thread-safe ({@link ObjectMapper} immutable setelah dibuat).
 */
public final class BudgetJsonCodec {

    private static final String UNKNOWN_LOCATION = "";

    private final ObjectMapper mapper = JsonMapper.builder(jsonFactory())
            // Skema ketat: field asing, duplikat key, token sisa, dan angka berupa string ditolak
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    /** Batas dari {@link SecurityLimits}: file berbahaya ditolak rapi, bukan bikin hang/OOM. */
    private static JsonFactory jsonFactory() {
        return JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxNestingDepth(SecurityLimits.JSON_MAX_NESTING_DEPTH)
                        .maxStringLength(SecurityLimits.JSON_MAX_STRING_LENGTH)
                        .maxNumberLength(SecurityLimits.JSON_MAX_NUMBER_LENGTH)
                        .build())
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                // Pesan error parser tidak boleh memuat potongan isi file (Security Requirements #5)
                .disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                // Nominal ditulis 1250000.50, bukan 1.2500005E+6
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .build();
    }

    /**
     * Ukuran dicek sebelum dibaca, lalu jumlah item dicek setelah skema valid (Security Requirements #19).
     *
     * @throws BudgetFileTooLargeException bila melebihi batas ukuran
     * @throws InvalidBudgetFileException bila tidak sesuai skema
     * @throws BudgetLimitExceededException bila jumlah item melebihi batas
     * @throws BudgetStorageException bila file gagal dibaca
     */
    public Budget read(Path file) {
        String fileName = file.getFileName().toString();
        try {
            byte[] content = readLimited(file, fileName);
            BudgetDocument document = mapper.readValue(content, BudgetDocument.class);
            if (document == null) {
                throw new InvalidBudgetFileException(fileName, Problem.MISSING_VALUE, UNKNOWN_LOCATION, null);
            }
            Budget budget = BudgetDocumentMapper.toDomain(document, fileName);
            if (budget.itemCount() > SecurityLimits.BUDGET_MAX_ITEMS) {
                throw new BudgetLimitExceededException(budget.itemCount(), SecurityLimits.BUDGET_MAX_ITEMS);
            }
            return budget;
        } catch (JsonMappingException e) {
            throw new InvalidBudgetFileException(fileName, mappingProblem(e), location(e), e);
        } catch (JsonProcessingException e) {
            throw new InvalidBudgetFileException(fileName, Problem.MALFORMED_JSON, UNKNOWN_LOCATION, e);
        } catch (IOException e) {
            throw new BudgetStorageException(fileName, e);
        }
    }

    /** Tulis ke {@code file} apa adanya; atomic/owner-only diurus pemanggil. */
    public void write(Budget budget, Path file) throws IOException {
        try (OutputStream output = Files.newOutputStream(file)) {
            mapper.writeValue(output, BudgetDocumentMapper.toDocument(budget));
        }
    }

    /** Baca maksimal batas + 1 byte, sehingga file yang membesar setelah dicek tetap tidak dibaca utuh. */
    private static byte[] readLimited(Path file, String fileName) throws IOException {
        long max = SecurityLimits.BUDGET_IMPORT_MAX_FILE_BYTES;
        long size = Files.size(file);
        if (size > max) {
            throw new BudgetFileTooLargeException(fileName, size, max);
        }
        try (InputStream input = Files.newInputStream(file)) {
            byte[] content = input.readNBytes((int) max + 1);
            if (content.length > max) {
                throw new BudgetFileTooLargeException(fileName, content.length, max);
            }
            return content;
        }
    }

    /** Token JSON salah tipe/format (misalnya satuan tak dikenal) dianggap nilai tidak valid, bukan JSON rusak. */
    private static Problem mappingProblem(JsonMappingException error) {
        return error.getPath().isEmpty() ? Problem.MALFORMED_JSON : Problem.INVALID_VALUE;
    }

    /** {@code sheets[0].items[3].price}: hanya nama field & index, tanpa isi. */
    private static String location(JsonMappingException error) {
        return error.getPath().stream()
                .map(reference -> reference.getFieldName() != null
                        ? "." + reference.getFieldName()
                        : "[" + reference.getIndex() + "]")
                .collect(Collectors.joining())
                .replaceFirst("^\\.", "");
    }
}
