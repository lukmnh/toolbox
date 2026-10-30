package io.github.lukmnh.toolbox.productivity.ambient.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.tool.Tool;
import io.github.lukmnh.toolbox.common.tool.ToolCategory;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.productivity.shared.presentation.ProductivityToolIds;
import java.util.Objects;
import java.util.Set;
import javafx.scene.Node;

/** Panel kontrol Ambient Sound. Pemutaran dimiliki {@link AmbientSoundService}, bukan tool ini. */
public final class AmbientTool implements Tool {

    private final AmbientSoundService service;
    private AmbientView view;

    public AmbientTool(AmbientSoundService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    public String id() {
        return ProductivityToolIds.AMBIENT;
    }

    @Override
    public String name() {
        return Messages.get("productivity.ambient.name");
    }

    @Override
    public String description() {
        return Messages.get("productivity.ambient.description");
    }

    @Override
    public ToolCategory category() {
        return ToolCategory.PRODUCTIVITY;
    }

    /** Suara dibangkitkan prosedural, tidak membuka file. */
    @Override
    public Set<String> openableExtensions() {
        return Set.of();
    }

    @Override
    public Node createView(ToolContext context) {
        dispose();
        view = new AmbientView(service);
        return view;
    }

    /** Hanya melepas binding view. Service sengaja tidak dihentikan supaya suara tetap jalan saat pindah tool. */
    @Override
    public void dispose() {
        if (view != null) {
            view.dispose();
            view = null;
        }
    }
}
