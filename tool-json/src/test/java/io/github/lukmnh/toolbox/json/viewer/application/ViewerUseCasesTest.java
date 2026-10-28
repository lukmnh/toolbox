package io.github.lukmnh.toolbox.json.viewer.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.common.task.ProgressReporter;
import io.github.lukmnh.toolbox.common.task.TaskCancelledException;
import io.github.lukmnh.toolbox.json.shared.domain.InvalidQueryException;
import io.github.lukmnh.toolbox.json.viewer.domain.ExportFormat;
import io.github.lukmnh.toolbox.json.viewer.domain.JsonFileFormat;
import io.github.lukmnh.toolbox.json.viewer.domain.RecentFiles;
import io.github.lukmnh.toolbox.json.viewer.domain.RecordColumn;
import io.github.lukmnh.toolbox.json.viewer.domain.SortSpec;
import io.github.lukmnh.toolbox.json.viewer.domain.ViewFilter;
import io.github.lukmnh.toolbox.json.viewer.infrastructure.FileViewExporter;
import io.github.lukmnh.toolbox.json.viewer.infrastructure.JacksonJsonFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Alur buka → filter → sort → export dengan file JSON asli di temp folder. */
class ViewerUseCasesTest {

    private static final String DATA = """
            [
              {"id": 1, "nama": "Ani", "kota": "Bandung", "umur": 30, "tags": ["a"]},
              {"id": 2, "nama": "Budi, S.", "kota": "Jakarta", "umur": 25},
              {"id": 3, "nama": "Citra \\"C\\"", "kota": "Bandung", "umur": null},
              {"id": 4, "nama": "Dodi", "kota": "bandung", "umur": 41, "extra": {"x": 1}}
            ]
            """;

    private final OpenJsonFileUseCase open = new OpenJsonFileUseCase(new JacksonJsonFiles());
    private final FilterRecordsUseCase filter = new FilterRecordsUseCase();
    private final SortRecordsUseCase sort = new SortRecordsUseCase();
    private final ExportViewUseCase export = new ExportViewUseCase(new FileViewExporter());

    @TempDir
    Path temp;

    private ViewerSession session;

    @BeforeEach
    void openSample() throws IOException {
        session = open.execute(
                Files.writeString(temp.resolve("orang.json"), DATA), ProgressReporter.NONE, CancellationToken.none());
    }

    @AfterEach
    void close() {
        session.close();
    }

    @Test
    void openSamplesColumnsInKeyOrder() {
        assertEquals(JsonFileFormat.ARRAY, session.format());
        assertEquals(4, session.recordCount());
        assertEquals(
                List.of("id", "nama", "kota", "umur", "tags", "extra"),
                session.columns().data().stream().map(RecordColumn::header).toList());
    }

    @Test
    void quickSearchAndQueryCombine() {
        assertArrayEquals(new int[] {0, 2, 3}, filter(new ViewFilter("BANDUNG", "")));
        assertArrayEquals(new int[] {0}, filter(new ViewFilter("bandung", "umur >= 30 AND kota = \"Bandung\"")));
        assertArrayEquals(new int[] {2}, filter(new ViewFilter("", "umur isNull")));
        assertArrayEquals(new int[] {0, 1, 2, 3}, filter(ViewFilter.NONE));
    }

    @Test
    void invalidQueryFailsBeforeScanning() {
        assertThrows(InvalidQueryException.class, () -> filter(new ViewFilter("", "umur >=")));
    }

    @Test
    void sortIsNumericWithNullsLastInBothDirections() {
        RecordColumn umur = new RecordColumn.Field("umur");
        int[] all = session.allRows();

        assertArrayEquals(new int[] {1, 0, 3, 2}, sort(all, new SortSpec(umur, true)));
        assertArrayEquals(new int[] {3, 0, 1, 2}, sort(all, new SortSpec(umur, false)));
        assertArrayEquals(new int[] {3, 2, 1, 0}, sort(all, new SortSpec(new RecordColumn.RecordNumber(), false)));
    }

    @Test
    void cancelledFilterStops() {
        CancellationToken token = new CancellationToken();
        token.cancel();

        assertThrows(
                TaskCancelledException.class,
                () -> filter.execute(session, new ViewFilter("a", ""), ProgressReporter.NONE, token));
    }

    @Test
    void exportCsvFollowsViewOrderAndRfc4180() throws IOException {
        int[] view = sort(
                filter(new ViewFilter("", "kota = \"Bandung\" OR kota = \"Jakarta\"")),
                new SortSpec(new RecordColumn.Field("id"), false));

        String csv = Files.readString(exportTo(view, ExportFormat.CSV));

        assertEquals(
                "id,nama,kota,umur,tags,extra\r\n"
                        + "3,\"Citra \"\"C\"\"\",Bandung,,,\r\n"
                        + "2,\"Budi, S.\",Jakarta,25,,\r\n"
                        + "1,Ani,Bandung,30,\"[\"\"a\"\"]\",\r\n",
                csv);
    }

    @Test
    void exportCsvNeutralizesFormulasButKeepsNumbers() throws IOException {
        session.close();
        session = open.execute(
                Files.writeString(temp.resolve("formula.json"), """
                        [{"=cmd": "=HYPERLINK(\\"x\\")", "saldo": "-12.5", "nilai": -3, "user": "@admin"}]
                        """), ProgressReporter.NONE, CancellationToken.none());

        String csv = Files.readString(exportTo(new int[] {0}, ExportFormat.CSV));

        assertEquals("'=cmd,saldo,nilai,user\r\n\"'=HYPERLINK(\"\"x\"\")\",-12.5,-3,'@admin\r\n", csv);
    }

    @Test
    void exportJsonArrayAndJsonLinesRoundTrip() throws IOException {
        int[] view = {3, 0};

        Path array = exportTo(view, ExportFormat.JSON_ARRAY);
        Path lines = exportTo(view, ExportFormat.JSON_LINES);

        try (ViewerSession reopened = open.execute(array, ProgressReporter.NONE, CancellationToken.none())) {
            assertEquals(2, reopened.recordCount());
            assertEquals(session.record(3), reopened.record(0));
        }
        List<String> jsonl = Files.readAllLines(lines);
        assertEquals(2, jsonl.size());
        assertTrue(jsonl.getFirst().startsWith("{\"id\": 4"), jsonl.getFirst());
    }

    @Test
    void exportOfEmptyViewIsValidJson() throws IOException {
        Path array = exportTo(new int[0], ExportFormat.JSON_ARRAY);

        assertEquals("[]\n", Files.readString(array));
    }

    @Test
    void recentFilesKeepNewestFirstWithoutDuplicatesUpToTen() {
        InMemoryRecent store = new InMemoryRecent();
        RecentJsonFilesUseCase recent = new RecentJsonFilesUseCase(store);
        for (int i = 0; i < 12; i++) {
            recent.remember(temp.resolve("f" + i + ".json"));
        }
        List<Path> result = recent.remember(temp.resolve("f5.json"));

        assertEquals(RecentFiles.MAX_FILES, result.size());
        assertEquals(temp.resolve("f5.json").toAbsolutePath(), result.getFirst());
        assertEquals(temp.resolve("f11.json").toAbsolutePath(), result.get(1));
        assertEquals(1, result.stream().filter(p -> p.endsWith("f5.json")).count());
    }

    private int[] filter(ViewFilter viewFilter) {
        return filter.execute(session, viewFilter, ProgressReporter.NONE, CancellationToken.none());
    }

    private int[] sort(int[] rows, SortSpec spec) {
        return sort.execute(session, rows, spec, ProgressReporter.NONE, CancellationToken.none());
    }

    private Path exportTo(int[] rows, ExportFormat format) throws IOException {
        Path output = Files.createTempFile(temp, "export", "." + format.name());
        return export.execute(session, rows, format, output, ProgressReporter.NONE, CancellationToken.none());
    }

    private static final class InMemoryRecent implements RecentFiles {
        private List<Path> files = new ArrayList<>();

        @Override
        public List<Path> load() {
            return files;
        }

        @Override
        public void save(List<Path> updated) {
            files = List.copyOf(updated);
        }
    }
}
