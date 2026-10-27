package io.github.lukmnh.toolbox.common.tool;

import java.nio.file.Path;
import java.util.Set;
import javafx.scene.Node;

/**
 * Kontrak plugin untuk satu tool. Shell hanya mengenal interface ini.
 *
 * <p>Siklus hidup: {@link #createView} dipanggil lazy saat tool dibuka, lalu {@link #dispose} saat
 * pindah tool atau aplikasi ditutup. Setelah {@code dispose}, {@code createView} boleh dipanggil lagi.
 */
public interface Tool {

    /** ID unik dan stabil, misalnya {@code "pdf.sign"}. Dipakai untuk routing dan recent. */
    String id();

    String name();

    String description();

    ToolCategory category();

    /** Ekstensi (huruf kecil, tanpa titik) yang bisa dibuka lewat "Open with". Kosong jika tidak ada. */
    Set<String> openableExtensions();

    Node createView(ToolContext context);

    /** Dipanggil setelah {@link #createView} bila tool dibuka dari argumen file. */
    default void openFile(Path file) {}

    /** {@code false} jika ada perubahan yang belum di-download. */
    default boolean canClose() {
        return true;
    }

    /** Lepas semua resource: dokumen, cache, file handle, temp file, dan batalkan task. */
    void dispose();
}
