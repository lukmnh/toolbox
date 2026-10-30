package io.github.lukmnh.toolbox.productivity.text.application;

import io.github.lukmnh.toolbox.common.task.CancellationToken;
import io.github.lukmnh.toolbox.productivity.text.domain.TextTransformation;

/** Terapkan satu transformasi (ubah huruf / rapikan) ke teks. */
public final class TransformTextUseCase {

    /** @return teks baru; teks asal tidak diubah */
    public String execute(String text, TextTransformation transformation, CancellationToken cancellation) {
        // Transformasi sendiri tidak bisa disela; cek sebelum & sesudah supaya hasil usang tidak dipakai
        cancellation.throwIfCancelled();
        String result = transformation.apply(text);
        cancellation.throwIfCancelled();
        return result;
    }
}
