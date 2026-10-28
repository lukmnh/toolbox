package io.github.lukmnh.toolbox.devutils.diff.domain;

import java.util.List;

/** Port ke library algoritma diff (Myers). */
public interface SequenceDiffer {

    /**
     * @param checkpoint dipanggil berkala selama perhitungan; boleh melempar exception untuk membatalkan
     * @return potongan yang berbeda saja (tanpa bagian yang sama), terurut dari awal
     */
    <T> List<DiffChunk> diff(List<T> original, List<T> revised, Runnable checkpoint);
}
