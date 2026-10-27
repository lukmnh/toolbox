package io.github.lukmnh.toolbox.json.viewer.domain;

import io.github.lukmnh.toolbox.json.shared.domain.ScanProgress;
import java.nio.file.Path;
import java.util.List;

/** Port: tulis record hasil view ke file secara streaming. */
public interface ViewExporter {

    /**
     * @param rows nomor record (index 0) sesuai urutan view
     * @param columns kolom CSV; diabaikan untuk format JSON
     */
    void export(
            RecordSource source,
            int[] rows,
            List<RecordColumn> columns,
            ExportFormat format,
            Path output,
            ScanProgress progress);
}
