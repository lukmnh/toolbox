package io.github.lukmnh.toolbox.app.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.scene.Node;
import org.junit.jupiter.api.Test;

class OpenWithRouterTest {

    private final StubTool pdfTool = new StubTool("pdf.sign", Set.of("pdf"));
    private final StubTool jsonTool = new StubTool("json.viewer", Set.of("json", "jsonl", "ndjson"));
    private final OpenWithRouter router =
            new OpenWithRouter(List.of(new StubTool("pdf.merge", Set.of()), pdfTool, jsonTool));

    @Test
    void pdfOpensSignToolWithTheFile() {
        List<String> warnings = new ArrayList<>();

        router.route(List.of("/tmp/Kontrak.PDF"), tool -> true, warnings::add);

        assertEquals(List.of(Path.of("/tmp/Kontrak.PDF")), pdfTool.opened);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void jsonVariantsOpenJsonViewer() {
        router.route(List.of("data.ndjson"), tool -> true, message -> {});

        assertEquals(List.of(Path.of("data.ndjson")), jsonTool.opened);
    }

    @Test
    void unsupportedExtensionWarnsAndOpensNothing() {
        List<String> warnings = new ArrayList<>();

        router.route(List.of("foto.png"), tool -> true, warnings::add);

        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("foto.png"));
        assertTrue(pdfTool.opened.isEmpty());
    }

    @Test
    void fileIsNotPassedWhenToolCouldNotBeOpened() {
        router.route(List.of("a.pdf"), tool -> false, message -> {});

        assertTrue(pdfTool.opened.isEmpty());
    }

    @Test
    void noArgumentsDoesNothing() {
        List<String> warnings = new ArrayList<>();

        router.route(List.of(), tool -> true, warnings::add);

        assertTrue(warnings.isEmpty());
    }

    private static final class StubTool implements Tool {
        private final String id;
        private final Set<String> extensions;
        final List<Path> opened = new ArrayList<>();

        StubTool(String id, Set<String> extensions) {
            this.id = id;
            this.extensions = extensions;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String name() {
            return id;
        }

        @Override
        public String description() {
            return id;
        }

        @Override
        public ToolCategory category() {
            return ToolCategory.PDF;
        }

        @Override
        public Set<String> openableExtensions() {
            return extensions;
        }

        @Override
        public Node createView(ToolContext context) {
            throw new UnsupportedOperationException("Tidak dipakai di test routing");
        }

        @Override
        public void openFile(Path file) {
            opened.add(file);
        }

        @Override
        public void dispose() {}
    }
}
