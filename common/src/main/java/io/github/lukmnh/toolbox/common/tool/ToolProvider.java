package io.github.lukmnh.toolbox.common.tool;

import java.util.List;

/**
 * Composition root sebuah module tool. Didaftarkan lewat {@link java.util.ServiceLoader} di
 * {@code META-INF/services/io.github.lukmnh.toolbox.common.tool.ToolProvider}.
 *
 * <p>Satu provider per module karena ServiceLoader butuh konstruktor tanpa argumen, sedangkan tool
 * dirakit dengan constructor injection.
 */
public interface ToolProvider {

    List<Tool> tools();
}
