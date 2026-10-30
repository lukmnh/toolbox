package io.github.lukmnh.toolbox.productivity.calculator.presentation;

import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Alur export bersama tab Kalkulator: tulis ke temp file di background, lalu serahkan ke Download/Simpan sebagai.
 * Gagal tulis ditampilkan lewat dialog error, tidak pernah diam-diam.
 */
final class FileExport {

    private final ToolContext context;
    private final TempFiles tempFiles;
    private final SimpleBooleanProperty busy = new SimpleBooleanProperty();
    /** Satu temp file per ekstensi dipakai ulang supaya download berulang tidak menumpuk di disk. */
    private final Map<String, Path> outputFiles = new HashMap<>();

    private RunningTask runningTask;

    FileExport(ToolContext context, TempFiles tempFiles) {
        this.context = context;
        this.tempFiles = tempFiles;
    }

    ReadOnlyBooleanProperty busyProperty() {
        return busy;
    }

    /**
     * @param suffix ekstensi temp file, misalnya {@code .pdf}
     * @param writer dijalankan di background; melempar exception domain bila gagal
     * @param deliver dijalankan di FX thread dengan file yang sudah ditulis
     */
    void run(String suffix, Consumer<Path> writer, Consumer<Path> deliver) {
        if (busy.get()) {
            return;
        }
        Path target = outputFiles.computeIfAbsent(suffix, tempFiles::createFile);
        busy.set(true);
        runningTask = context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            writer.accept(target);
                            return target;
                        },
                        file -> {
                            busy.set(false);
                            deliver.accept(file);
                        },
                        failure -> {
                            busy.set(false);
                            CalculatorMessages.showError(context.owner(), failure);
                        });
    }

    void dispose() {
        if (runningTask != null) {
            runningTask.cancel();
        }
        tempFiles.close();
    }
}
