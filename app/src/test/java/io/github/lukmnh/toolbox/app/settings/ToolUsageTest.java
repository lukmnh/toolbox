package io.github.lukmnh.toolbox.app.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import java.util.List;
import java.util.Set;
import javafx.scene.Node;
import org.junit.jupiter.api.Test;

class ToolUsageTest {

    @Test
    void openedToolMovesToFrontWithoutDuplicates() {
        ToolUsage usage = ToolUsage.EMPTY.withOpened("a").withOpened("b").withOpened("a");

        assertEquals(List.of("a", "b"), usage.recent());
    }

    @Test
    void recentKeepsOnlyNewestEntries() {
        ToolUsage usage = ToolUsage.EMPTY;
        for (int i = 0; i < ToolUsage.MAX_RECENT + 2; i++) {
            usage = usage.withOpened("tool" + i);
        }

        assertEquals(ToolUsage.MAX_RECENT, usage.recent().size());
        assertEquals("tool7", usage.recent().getFirst());
    }

    @Test
    void togglingFavoriteTwiceRemovesIt() {
        ToolUsage pinned = ToolUsage.EMPTY.withFavoriteToggled("a");
        assertTrue(pinned.isFavorite("a"));

        assertFalse(pinned.withFavoriteToggled("a").isFavorite("a"));
    }

    @Test
    void withoutRecentKeepsFavorites() {
        ToolUsage usage =
                ToolUsage.EMPTY.withFavoriteToggled("a").withOpened("b").withoutRecent();

        assertEquals(new ToolUsage(List.of("a"), List.of()), usage);
    }

    @Test
    void resolveKeepsOrderAndSkipsUnknownIds() {
        Tool first = new StubTool("first");
        Tool second = new StubTool("second");

        assertEquals(
                List.of(second, first), ToolUsage.resolve(List.of("second", "gone", "first"), List.of(first, second)));
    }

    private record StubTool(String id) implements Tool {

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
            return ToolCategory.DEV_UTILS;
        }

        @Override
        public Set<String> openableExtensions() {
            return Set.of();
        }

        @Override
        public Node createView(ToolContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void dispose() {}
    }
}
