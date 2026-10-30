package io.github.lukmnh.toolbox.devutils.qr.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.io.TempFiles;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Debounce;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.devutils.qr.application.GenerateQrUseCase;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrErrorCorrection;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrMatrix;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrToolException;
import io.github.lukmnh.toolbox.devutils.qr.domain.QrUrl;
import io.github.lukmnh.toolbox.devutils.qr.domain.VCard;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

/** State dan aksi QR Code Generator. Preview diperbarui (debounce) setiap isi atau opsi berubah. FX thread. */
final class QrViewModel {

    static final int DEFAULT_SIZE = 512;
    private static final Duration DEBOUNCE_DELAY = Duration.millis(250);
    private static final String PNG_NAME = "qr-code.png";

    private final QrUseCases useCases;
    private final ToolContext context;
    private final TempFiles tempFiles;

    private final ObjectProperty<QrMode> mode = new SimpleObjectProperty<>(QrMode.TEXT);
    private final StringProperty text = new SimpleStringProperty("");
    private final StringProperty url = new SimpleStringProperty("");
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty organization = new SimpleStringProperty("");
    private final StringProperty title = new SimpleStringProperty("");
    private final StringProperty phone = new SimpleStringProperty("");
    private final StringProperty email = new SimpleStringProperty("");
    private final StringProperty website = new SimpleStringProperty("");
    private final StringProperty address = new SimpleStringProperty("");

    private final ObjectProperty<QrErrorCorrection> errorCorrection = new SimpleObjectProperty<>(QrErrorCorrection.M);
    private final ObjectProperty<Integer> size = new SimpleObjectProperty<>(DEFAULT_SIZE);
    private final ObjectProperty<Integer> margin = new SimpleObjectProperty<>(GenerateQrUseCase.DEFAULT_MARGIN);

    private final ObjectProperty<QrMatrix> matrix = new SimpleObjectProperty<>();
    private final StringProperty payload = new SimpleStringProperty("");
    private final StringProperty message = new SimpleStringProperty("");
    private final ObjectProperty<Tone> messageTone = new SimpleObjectProperty<>(Tone.NEUTRAL);

    private final Debounce debounce = new Debounce(DEBOUNCE_DELAY, this::refresh);
    /** Satu temp file dipakai ulang supaya download berulang tidak menumpuk di disk. */
    private Path outputFile;

    QrViewModel(QrUseCases useCases, ToolContext context, TempFiles tempFiles) {
        this.useCases = useCases;
        this.context = context;
        this.tempFiles = tempFiles;
        // Ukuran PNG tidak mengubah matriks, jadi tidak perlu encode ulang
        for (Observable source : List.<Observable>of(
                mode, text, url, name, organization, title, phone, email, website, address, errorCorrection, margin)) {
            source.addListener(observable -> debounce.trigger());
        }
        refresh();
    }

    ObjectProperty<QrMode> modeProperty() {
        return mode;
    }

    StringProperty textProperty() {
        return text;
    }

    StringProperty urlProperty() {
        return url;
    }

    StringProperty nameProperty() {
        return name;
    }

    StringProperty organizationProperty() {
        return organization;
    }

    StringProperty titleProperty() {
        return title;
    }

    StringProperty phoneProperty() {
        return phone;
    }

    StringProperty emailProperty() {
        return email;
    }

    StringProperty websiteProperty() {
        return website;
    }

    StringProperty addressProperty() {
        return address;
    }

    ObjectProperty<QrErrorCorrection> errorCorrectionProperty() {
        return errorCorrection;
    }

    ObjectProperty<Integer> sizeProperty() {
        return size;
    }

    ObjectProperty<Integer> marginProperty() {
        return margin;
    }

    ReadOnlyObjectProperty<QrMatrix> matrixProperty() {
        return matrix;
    }

    ReadOnlyStringProperty payloadProperty() {
        return payload;
    }

    ReadOnlyStringProperty messageProperty() {
        return message;
    }

    ReadOnlyObjectProperty<Tone> messageToneProperty() {
        return messageTone;
    }

    /** Ringkasan matriks dan ukuran PNG yang akan di-download; kosong bila belum ada QR. */
    String statsText() {
        QrMatrix current = matrix.get();
        if (current == null) {
            return "";
        }
        return Messages.format(
                "devutils.qr.stats",
                String.valueOf(current.dimension()),
                String.valueOf(current.renderedSide(size.get())),
                String.valueOf(payload.get().getBytes(StandardCharsets.UTF_8).length));
    }

    void copyPayload() {
        ClipboardActions.copy(payload.get(), context.notifier());
    }

    void download() {
        export((file, fileName) -> context.downloads().download(file, fileName));
    }

    void saveAs() {
        export((file, fileName) -> context.downloads().saveAs(file, fileName));
    }

    void dispose() {
        debounce.stop();
        tempFiles.close();
    }

    /** Encode cepat (milidetik) sehingga dijalankan langsung di FX thread setelah debounce. */
    private void refresh() {
        try {
            Optional<String> content = buildPayload();
            if (content.isEmpty()) {
                show(null, "", Messages.get(mode.get().emptyKey()), Tone.NEUTRAL);
                return;
            }
            QrMatrix encoded = useCases.generate().execute(content.get(), errorCorrection.get(), margin.get());
            show(encoded, content.get(), "", Tone.NEUTRAL);
        } catch (QrToolException e) {
            show(null, "", QrErrorMessages.describe(e), Tone.DANGER);
        }
    }

    /** @return kosong bila isi mode aktif belum diisi (bukan error) */
    private Optional<String> buildPayload() {
        return switch (mode.get()) {
            case TEXT -> text.get().isEmpty() ? Optional.empty() : Optional.of(text.get());
            case URL -> url.get().isBlank() ? Optional.empty() : Optional.of(QrUrl.validate(url.get()));
            case VCARD -> {
                VCard card = new VCard(
                        name.get(),
                        organization.get(),
                        title.get(),
                        phone.get(),
                        email.get(),
                        website.get(),
                        address.get());
                yield card.hasName() ? Optional.of(card.toPayload()) : Optional.empty();
            }
        };
    }

    private void show(QrMatrix encoded, String content, String messageText, Tone tone) {
        payload.set(content);
        matrix.set(encoded);
        messageTone.set(tone);
        message.set(messageText);
    }

    private void export(BiConsumer<Path, String> deliver) {
        QrMatrix current = matrix.get();
        if (current == null) {
            return;
        }
        int side = size.get();
        Path target = outputFile();
        context.tasks()
                .submit(
                        (progress, cancellation) -> {
                            useCases.exportPng().execute(current, side, target);
                            return target;
                        },
                        file -> deliver.accept(file, PNG_NAME),
                        error -> QrErrorMessages.showExportFailure(context.owner(), error));
    }

    private Path outputFile() {
        if (outputFile == null) {
            outputFile = tempFiles.createFile(".png");
        }
        return outputFile;
    }
}
