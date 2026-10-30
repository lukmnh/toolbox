package io.github.lukmnh.toolbox.devutils.hash.presentation;

import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.devutils.hash.domain.KeyPairAlgorithm;
import io.github.lukmnh.toolbox.devutils.hash.domain.PemKeyPair;
import io.github.lukmnh.toolbox.devutils.hash.domain.SecretEncoding;
import io.github.lukmnh.toolbox.devutils.hash.domain.UuidVersion;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * State dan aksi tab Generate: random secret, key pair PEM, dan UUID. Nilai langsung dibuat saat tab pertama kali
 * dibuka dan setiap opsi berubah, tanpa input dari user. Semua method dipanggil dari FX thread.
 */
final class GenerateViewModel {

    static final int DEFAULT_SECRET_BYTES = 32;
    static final int DEFAULT_UUID_COUNT = 5;

    /** Jenis key untuk download; menentukan isi dan nama file. */
    enum KeyPart {
        PUBLIC("public"),
        PRIVATE("private");

        private final String fileSuffix;

        KeyPart(String fileSuffix) {
            this.fileSuffix = fileSuffix;
        }

        String fileName(KeyPairAlgorithm algorithm) {
            return algorithm.fileSlug() + "-" + fileSuffix + ".pem";
        }

        String pem(PemKeyPair keyPair) {
            return this == PUBLIC ? keyPair.publicKeyPem() : keyPair.privateKeyPem();
        }
    }

    private final HashUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;

    private final ObjectProperty<Integer> secretBytes = new SimpleObjectProperty<>(DEFAULT_SECRET_BYTES);
    private final ObjectProperty<SecretEncoding> secretEncoding = new SimpleObjectProperty<>(SecretEncoding.HEX);
    private final StringProperty secret = new SimpleStringProperty("");

    private final ObjectProperty<KeyPairAlgorithm> keyAlgorithm = new SimpleObjectProperty<>(KeyPairAlgorithm.RSA_2048);
    private final ObjectProperty<PemKeyPair> keyPair = new SimpleObjectProperty<>();
    private final BooleanProperty keyBusy = new SimpleBooleanProperty();
    private final ObjectProperty<RunningTask> keyTask = new SimpleObjectProperty<>();

    private final ObjectProperty<UuidVersion> uuidVersion = new SimpleObjectProperty<>(UuidVersion.V4);
    private final ObjectProperty<Integer> uuidCount = new SimpleObjectProperty<>(DEFAULT_UUID_COUNT);
    private final StringProperty uuids = new SimpleStringProperty("");

    /** Satu temp file per jenis key dipakai ulang supaya download berulang tidak menumpuk di disk. */
    private final Map<KeyPart, Path> exportFiles = new EnumMap<>(KeyPart.class);

    private boolean started;
    private RunningTask runningKeyTask;

    GenerateViewModel(HashUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
        secretBytes.addListener((observable, previous, current) -> regenerateSecretIfStarted());
        secretEncoding.addListener((observable, previous, current) -> regenerateSecretIfStarted());
        keyAlgorithm.addListener((observable, previous, current) -> {
            if (started) {
                regenerateKeyPair();
            }
        });
        uuidVersion.addListener((observable, previous, current) -> regenerateUuidsIfStarted());
        uuidCount.addListener((observable, previous, current) -> regenerateUuidsIfStarted());
    }

    ObjectProperty<Integer> secretBytesProperty() {
        return secretBytes;
    }

    ObjectProperty<SecretEncoding> secretEncodingProperty() {
        return secretEncoding;
    }

    ReadOnlyStringProperty secretProperty() {
        return secret;
    }

    ObjectProperty<KeyPairAlgorithm> keyAlgorithmProperty() {
        return keyAlgorithm;
    }

    ReadOnlyObjectProperty<PemKeyPair> keyPairProperty() {
        return keyPair;
    }

    ReadOnlyBooleanProperty keyBusyProperty() {
        return keyBusy;
    }

    ReadOnlyObjectProperty<RunningTask> keyTaskProperty() {
        return keyTask;
    }

    ObjectProperty<UuidVersion> uuidVersionProperty() {
        return uuidVersion;
    }

    ObjectProperty<Integer> uuidCountProperty() {
        return uuidCount;
    }

    ReadOnlyStringProperty uuidsProperty() {
        return uuids;
    }

    /** Dipanggil saat tab Generate dibuka; hanya generate sekali supaya nilai tidak berganti tiap pindah tab. */
    void ensureGenerated() {
        if (started) {
            return;
        }
        started = true;
        regenerateSecret();
        regenerateKeyPair();
        regenerateUuids();
    }

    void regenerateSecret() {
        secret.set(useCases.secret().execute(secretBytes.get(), secretEncoding.get()));
    }

    /** Key pair dibuat di background; RSA 4096 bisa makan beberapa detik. */
    void regenerateKeyPair() {
        cancelKeyTask();
        KeyPairAlgorithm algorithm = keyAlgorithm.get();
        keyPair.set(null);
        RunningTask task = context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.keyPair().execute(algorithm, cancellation),
                        keyPair::set,
                        error -> GenerateErrorMessages.showKeyPairFailure(context.owner(), error));
        runningKeyTask = task;
        keyBusy.set(true);
        task.runningProperty().addListener((observable, wasRunning, running) -> {
            // Task lama yang dibatalkan tidak boleh mematikan indikator milik task baru
            if (task == runningKeyTask) {
                keyBusy.set(running);
            }
        });
        keyTask.set(task);
    }

    void regenerateUuids() {
        uuids.set(useCases.uuids().execute(uuidVersion.get(), uuidCount.get()).stream()
                .map(UUID::toString)
                .collect(Collectors.joining("\n")));
    }

    String keyText(KeyPart part) {
        PemKeyPair current = keyPair.get();
        return current == null ? "" : part.pem(current);
    }

    void download(KeyPart part) {
        export(part, (file, name) -> context.downloads().download(file, name));
    }

    void saveAs(KeyPart part) {
        export(part, (file, name) -> context.downloads().saveAs(file, name));
    }

    void dispose() {
        cancelKeyTask();
        // Temp berisi private key ikut dihapus saat tool ditutup
        tempFiles.close();
        exportFiles.clear();
    }

    private void export(KeyPart part, BiConsumer<Path, String> deliver) {
        PemKeyPair current = keyPair.get();
        if (current == null) {
            return;
        }
        String pem = part.pem(current);
        String name = part.fileName(current.algorithm());
        Path target = exportFiles.computeIfAbsent(part, ignored -> tempFiles.createFile(".pem"));
        context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            useCases.exportPem().execute(pem, target);
                            return target;
                        },
                        file -> deliver.accept(file, name),
                        error -> GenerateErrorMessages.showExportFailure(context.owner(), error));
    }

    private void regenerateSecretIfStarted() {
        if (started) {
            regenerateSecret();
        }
    }

    private void regenerateUuidsIfStarted() {
        if (started) {
            regenerateUuids();
        }
    }

    private void cancelKeyTask() {
        if (runningKeyTask != null) {
            runningKeyTask.cancel();
            runningKeyTask = null;
        }
        keyBusy.set(false);
    }
}
