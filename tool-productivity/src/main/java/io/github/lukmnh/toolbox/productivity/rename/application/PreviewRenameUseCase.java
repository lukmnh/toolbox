package io.github.lukmnh.toolbox.productivity.rename.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.productivity.rename.domain.FileBatch;
import io.github.lukmnh.toolbox.productivity.rename.domain.InvalidRuleException;
import io.github.lukmnh.toolbox.productivity.rename.domain.PatternReplacer;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameEngine;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameFileSystem;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenamePreview;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameRule;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Hitung nama baru + status tiap file terhadap isi folder saat ini. Tidak mengubah file apa pun. */
public final class PreviewRenameUseCase {

    private final RenameFileSystem fileSystem;
    private final PatternReplacer replacer;
    private final ZoneId zone;

    /** @param zone zona waktu untuk aturan sisip tanggal modifikasi */
    public PreviewRenameUseCase(RenameFileSystem fileSystem, PatternReplacer replacer, ZoneId zone) {
        this.fileSystem = fileSystem;
        this.replacer = replacer;
        this.zone = zone;
    }

    /**
     * @param rules aturan aktif, diterapkan berurutan
     * @throws InvalidRuleException bila ada aturan yang tidak bisa diterapkan
     */
    public RenamePreview execute(FileBatch batch, List<RenameRule> rules, CancellationToken cancellation) {
        RenameEngine engine = new RenameEngine(rules, replacer, zone);
        List<String> newNames = new ArrayList<>(batch.files().size());
        for (int index = 0; index < batch.files().size(); index++) {
            cancellation.throwIfCancelled();
            newNames.add(engine.newName(batch.files().get(index), index));
        }
        Set<String> folderNames = fileSystem.entryNames(batch.folder());
        return RenamePreview.of(batch, newNames, folderNames, fileSystem.caseInsensitiveNames());
    }
}
