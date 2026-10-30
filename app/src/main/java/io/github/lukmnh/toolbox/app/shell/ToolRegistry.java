package io.github.lukmnh.toolbox.app.shell;

import io.github.lukmnh.toolbox.common.tool.BackgroundService;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolProvider;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Memuat semua tool dan background service dari module yang ada di classpath. Shell tidak meng-hard-code daftar
 * tool. Provider di-load sekali supaya tool dan service dari module yang sama memakai instance yang sama.
 */
public record ToolRegistry(List<Tool> tools, List<BackgroundService> backgroundServices) {

    public ToolRegistry {
        tools = List.copyOf(tools);
        backgroundServices = List.copyOf(backgroundServices);
    }

    public static ToolRegistry load() {
        List<ToolProvider> providers = ServiceLoader.load(ToolProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
        List<Tool> tools = providers.stream()
                .flatMap(provider -> provider.tools().stream())
                // Urutan stabil: per kategori sesuai enum, lalu urutan dari provider
                .sorted(Comparator.comparing(Tool::category))
                .toList();
        List<BackgroundService> services = providers.stream()
                .flatMap(provider -> provider.backgroundServices().stream())
                .toList();
        return new ToolRegistry(tools, services);
    }
}
