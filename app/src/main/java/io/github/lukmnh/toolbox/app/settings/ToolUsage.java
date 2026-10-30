package io.github.lukmnh.toolbox.app.settings;

import io.github.lukmnh.toolbox.common.tool.Tool;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Tool favorit (urutan pin) dan tool terakhir dibuka (terbaru di depan). Immutable; ubah dengan method {@code with...}. */
public record ToolUsage(List<String> favorites, List<String> recent) {

    public static final int MAX_RECENT = 6;
    public static final ToolUsage EMPTY = new ToolUsage(List.of(), List.of());

    public ToolUsage {
        favorites = List.copyOf(new LinkedHashSet<>(favorites));
        recent = recent.stream().distinct().limit(MAX_RECENT).toList();
    }

    public boolean isFavorite(String toolId) {
        return favorites.contains(toolId);
    }

    /** Taruh tool di posisi paling depan recent; yang paling lama terbuang bila lewat {@link #MAX_RECENT}. */
    public ToolUsage withOpened(String toolId) {
        List<String> updated = new ArrayList<>(MAX_RECENT);
        updated.add(toolId);
        recent.stream().filter(id -> !id.equals(toolId)).forEach(updated::add);
        return new ToolUsage(favorites, updated);
    }

    public ToolUsage withFavoriteToggled(String toolId) {
        List<String> updated = new ArrayList<>(favorites);
        if (!updated.remove(toolId)) {
            updated.add(toolId);
        }
        return new ToolUsage(updated, recent);
    }

    public ToolUsage withoutRecent() {
        return new ToolUsage(favorites, List.of());
    }

    /** Tool sesuai urutan {@code ids}; id yang tool-nya sudah tidak terpasang dilewati. */
    public static List<Tool> resolve(List<String> ids, List<Tool> tools) {
        return ids.stream()
                .flatMap(id ->
                        tools.stream().filter(tool -> tool.id().equals(id)).limit(1))
                .toList();
    }
}
