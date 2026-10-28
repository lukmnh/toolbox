package io.github.lukmnh.toolbox.devutils.hash.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.task.BackgroundWork;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.devutils.hash.application.HashTextUseCase;
import io.github.lukmnh.toolbox.devutils.hash.application.HmacTextUseCase;
import io.github.lukmnh.toolbox.devutils.hash.domain.DigestEncoding;
import io.github.lukmnh.toolbox.devutils.hash.domain.DigestResult;
import io.github.lukmnh.toolbox.devutils.shared.presentation.Debounce;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

/** State dan aksi Hash Generator. Semua method dipanggil dari FX thread. */
public final class HashViewModel {

    private static final Duration DEBOUNCE_DELAY = Duration.millis(200);

    private final HashTextUseCase hashUseCase;
    private final HmacTextUseCase hmacUseCase;
    private final ToolContext context;

    private final StringProperty text = new SimpleStringProperty("");
    private final ObjectProperty<HashMode> mode = new SimpleObjectProperty<>(HashMode.PLAIN);
    private final ObjectProperty<DigestEncoding> encoding = new SimpleObjectProperty<>(DigestEncoding.HEX_LOWER);
    private final ObjectProperty<Outcome> outcome = new SimpleObjectProperty<>();
    private final Debounce debounce = new Debounce(DEBOUNCE_DELAY, this::hashText);
    private RunningTask runningTask;
    /** Key HMAC yang sedang dipakai task; di-wipe juga di sini kalau task batal sebelum sempat jalan. */
    private char[] pendingKey;

    public HashViewModel(HashTextUseCase hashUseCase, HmacTextUseCase hmacUseCase, ToolContext context) {
        this.hashUseCase = hashUseCase;
        this.hmacUseCase = hmacUseCase;
        this.context = context;
        text.addListener((observable, previous, current) -> onInputChanged());
        mode.addListener((observable, previous, current) -> onInputChanged());
    }

    public StringProperty textProperty() {
        return text;
    }

    ObjectProperty<HashMode> modeProperty() {
        return mode;
    }

    public ObjectProperty<DigestEncoding> encodingProperty() {
        return encoding;
    }

    public ReadOnlyObjectProperty<Outcome> outcomeProperty() {
        return outcome;
    }

    /**
     * Hitung HMAC teks saat ini. Pemilik array berpindah ke view model: array selalu di-wipe setelah dipakai atau
     * saat dibatalkan.
     */
    public void computeHmac(char[] key) {
        debounce.stop();
        cancelRunning();
        pendingKey = key;
        String snapshot = text.get();
        submit((progress, cancellation) -> outcomeOf(snapshot, hmacUseCase.execute(snapshot, key, cancellation), true));
    }

    public String statsText(Outcome current) {
        return Messages.format("devutils.hash.stats", current.characters(), current.bytes());
    }

    public void dispose() {
        debounce.stop();
        cancelRunning();
    }

    /** Hasil HMAC tidak lagi sesuai teks/mode baru; key sudah di-wipe sehingga user harus menekan tombol lagi. */
    private void onInputChanged() {
        cancelRunning();
        outcome.set(null);
        if (mode.get() == HashMode.PLAIN) {
            debounce.trigger();
        } else {
            debounce.stop();
        }
    }

    private void hashText() {
        String snapshot = text.get();
        if (snapshot.isEmpty() || mode.get() != HashMode.PLAIN) {
            return;
        }
        submit((progress, cancellation) -> outcomeOf(snapshot, hashUseCase.execute(snapshot, cancellation), false));
    }

    private void submit(BackgroundWork<Outcome> work) {
        cancelRunning();
        runningTask = context.tasks()
                .submit(
                        work,
                        result -> {
                            wipePendingKey();
                            outcome.set(result);
                        },
                        this::showError);
    }

    private static Outcome outcomeOf(String snapshot, List<DigestResult> results, boolean hmac) {
        long characters = snapshot.codePointCount(0, snapshot.length());
        long bytes = snapshot.getBytes(StandardCharsets.UTF_8).length;
        return new Outcome(results, characters, bytes, hmac);
    }

    private void showError(Throwable error) {
        wipePendingKey();
        ErrorDialogs.show(context.owner(), Messages.get("devutils.hash.error"), ErrorDialogs.describe(error));
    }

    private void cancelRunning() {
        if (runningTask != null) {
            runningTask.cancel();
            runningTask = null;
        }
        wipePendingKey();
    }

    private void wipePendingKey() {
        if (pendingKey != null) {
            Arrays.fill(pendingKey, '\0');
            pendingKey = null;
        }
    }

    /**
     * @param characters jumlah code point (emoji dihitung 1)
     * @param bytes panjang UTF-8
     * @param hmac {@code true} bila {@code results} hasil HMAC
     */
    public record Outcome(List<DigestResult> results, long characters, long bytes, boolean hmac) {

        public Outcome {
            results = List.copyOf(results);
        }
    }
}
