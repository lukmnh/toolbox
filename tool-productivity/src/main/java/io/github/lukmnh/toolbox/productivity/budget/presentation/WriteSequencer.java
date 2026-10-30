package io.github.lukmnh.toolbox.productivity.budget.presentation;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Mengurutkan penulisan file budget dari beberapa background task.
 *
 * <p>Autosave berjalan di virtual thread terpisah; tanpa penjaga, autosave lama yang selesai belakangan bisa
 * menimpa state yang lebih baru, atau menghidupkan lagi file yang baru di-rename/dihapus. Setiap penulisan membawa
 * nomor revisi; revisi yang sudah tersusul dilewati. {@link ReentrantLock} (bukan {@code synchronized}) supaya
 * virtual thread tidak ter-pin ke carrier thread selama I/O.
 */
final class WriteSequencer {

    private final ReentrantLock lock = new ReentrantLock();
    private long lastWritten;

    /** @return {@code false} bila revisi ini sudah tersusul penulisan yang lebih baru */
    boolean writeIfNewer(long revision, Runnable write) {
        lock.lock();
        try {
            if (revision <= lastWritten) {
                return false;
            }
            write.run();
            lastWritten = revision;
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Operasi struktural (rename, hapus, ganti budget). Revisi ditandai tertulis lebih dulu, sehingga autosave yang
     * masih antre untuk revisi ini atau lebih lama tidak menulis ulang file lama.
     */
    <T> T exclusive(long revision, Supplier<T> operation) {
        lock.lock();
        try {
            lastWritten = Math.max(lastWritten, revision);
            return operation.get();
        } finally {
            lock.unlock();
        }
    }
}
