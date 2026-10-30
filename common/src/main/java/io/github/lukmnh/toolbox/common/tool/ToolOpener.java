package io.github.lukmnh.toolbox.common.tool;

import java.nio.file.Path;

/** Membuka tool lain dari dalam sebuah tool, misalnya tombol "Buka di JSON Viewer". Diimplementasikan shell. */
@FunctionalInterface
public interface ToolOpener {

    /**
     * Pindah ke tool {@code toolId} lalu buka {@code file} di sana. Tool aktif di-dispose seperti pindah tool biasa,
     * jadi pemanggil tidak boleh memakai state view-nya setelah method ini.
     *
     * @throws IllegalArgumentException bila tidak ada tool dengan id tersebut
     */
    void open(String toolId, Path file);
}
