package io.github.lukmnh.toolbox.app.routing;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.FileExtensions;
import io.github.lukmnh.toolbox.common.tool.Tool;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** "Open with": argumen file saat launch diarahkan ke tool yang mendukung ekstensinya. */
public final class OpenWithRouter {

    private final List<Tool> tools;

    public OpenWithRouter(List<Tool> tools) {
        this.tools = List.copyOf(tools);
    }

    /** Tool pertama (sesuai urutan registry) yang mendukung ekstensi file. */
    public Optional<Tool> resolve(Path file) {
        String extension = FileExtensions.extensionOf(file);
        return tools.stream()
                .filter(tool -> tool.openableExtensions().contains(extension))
                .findFirst();
    }

    /**
     * @param openTool membuka tool; {@code false} bila batal
     * @param warn menampilkan pesan untuk tipe file yang tidak didukung
     */
    public void route(List<String> arguments, Predicate<Tool> openTool, Consumer<String> warn) {
        if (arguments.isEmpty()) {
            return;
        }
        Path file = Path.of(arguments.getFirst());
        Optional<Tool> target = resolve(file);
        if (target.isEmpty()) {
            warn.accept(Messages.format("app.openWith.unsupported", file.getFileName()));
            return;
        }
        Tool tool = target.get();
        if (openTool.test(tool)) {
            tool.openFile(file);
        }
    }
}
