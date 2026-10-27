package io.github.lukmnh.toolbox.app.shell;

import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolProvider;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/** Memuat semua tool dari module yang ada di classpath. Shell tidak meng-hard-code daftar tool. */
public final class ToolRegistry {

    private ToolRegistry() {}

    public static List<Tool> load() {
        return ServiceLoader.load(ToolProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .flatMap(provider -> provider.tools().stream())
                // Urutan stabil: per kategori sesuai enum, lalu urutan dari provider
                .sorted(Comparator.comparing(Tool::category))
                .toList();
    }
}
