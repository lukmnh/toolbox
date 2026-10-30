package io.github.lukmnh.toolbox.devutils.jwt.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.Debounce;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.devutils.jwt.application.DecodeJwtUseCase;
import io.github.lukmnh.toolbox.devutils.jwt.application.KeyInput;
import io.github.lukmnh.toolbox.devutils.jwt.application.VerifyJwtSignatureUseCase;
import io.github.lukmnh.toolbox.devutils.jwt.domain.Claim;
import io.github.lukmnh.toolbox.devutils.jwt.domain.DecodedJwt;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtAlgorithm;
import io.github.lukmnh.toolbox.devutils.jwt.domain.JwtToolException;
import io.github.lukmnh.toolbox.devutils.jwt.domain.SignatureStatus;
import io.github.lukmnh.toolbox.devutils.jwt.domain.TimeStatus;
import io.github.lukmnh.toolbox.devutils.jwt.domain.TimeValidity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** State dan aksi JWT Debugger. Token dan secret tidak pernah disimpan ke settings/recent atau di-log (#20). */
public final class JwtViewModel {

    private static final javafx.util.Duration DECODE_DELAY = javafx.util.Duration.millis(200);
    private static final int DEFAULT_SKEW_SECONDS = (int) TimeValidity.DEFAULT_CLOCK_SKEW.toSeconds();

    private final DecodeJwtUseCase decodeUseCase;
    private final VerifyJwtSignatureUseCase verifyUseCase;
    private final ToolContext context;
    private final Clock clock = Clock.systemUTC();
    private final ZoneId zone = ZoneId.systemDefault();
    private final Debounce debounce = new Debounce(DECODE_DELAY, this::decode);

    private final StringProperty token = new SimpleStringProperty("");
    private final ObjectProperty<DecodedJwt> decoded = new SimpleObjectProperty<>();
    private final StringProperty header = new SimpleStringProperty("");
    private final StringProperty payload = new SimpleStringProperty("");
    private final StringProperty signature = new SimpleStringProperty("");
    private final ObservableList<ClaimRow> claims = FXCollections.observableArrayList();
    private final ObjectProperty<StatusText> formatStatus = new SimpleObjectProperty<>(noToken());
    private final ObjectProperty<StatusText> timeStatus = new SimpleObjectProperty<>(noTime());
    private final ObjectProperty<StatusText> signatureStatus = new SimpleObjectProperty<>(notVerified());
    private final ObjectProperty<StatusText> algorithmStatus = new SimpleObjectProperty<>(unknownAlgorithm());
    private final StringProperty decodeError = new SimpleStringProperty("");
    private final StringProperty decodeWarning = new SimpleStringProperty("");
    private final ObjectProperty<KeyMode> keyMode = new SimpleObjectProperty<>(KeyMode.UNAVAILABLE);
    private final BooleanProperty secretBase64 = new SimpleBooleanProperty();
    private final StringProperty publicKeyText = new SimpleStringProperty("");
    private final ObjectProperty<Integer> clockSkewSeconds = new SimpleObjectProperty<>(DEFAULT_SKEW_SECONDS);
    private final ObjectProperty<StatusText> verifyResult = new SimpleObjectProperty<>(StatusText.EMPTY);
    private RunningTask runningTask;
    /** Hasil decode yang datang terlambat (token sudah diganti) diabaikan. */
    private long generation;

    public JwtViewModel(DecodeJwtUseCase decodeUseCase, VerifyJwtSignatureUseCase verifyUseCase, ToolContext context) {
        this.decodeUseCase = decodeUseCase;
        this.verifyUseCase = verifyUseCase;
        this.context = context;
        token.addListener((observable, previous, current) -> debounce.trigger());
        clockSkewSeconds.addListener((observable, previous, current) -> refreshTimeStatus());
        publicKeyText.addListener((observable, previous, current) -> resetVerification());
        secretBase64.addListener((observable, previous, current) -> resetVerification());
    }

    public StringProperty tokenProperty() {
        return token;
    }

    public ReadOnlyStringProperty headerProperty() {
        return header;
    }

    public ReadOnlyStringProperty payloadProperty() {
        return payload;
    }

    public ReadOnlyStringProperty signatureProperty() {
        return signature;
    }

    public ObservableList<ClaimRow> claims() {
        return claims;
    }

    ReadOnlyObjectProperty<StatusText> formatStatusProperty() {
        return formatStatus;
    }

    ReadOnlyObjectProperty<StatusText> timeStatusProperty() {
        return timeStatus;
    }

    ReadOnlyObjectProperty<StatusText> signatureStatusProperty() {
        return signatureStatus;
    }

    ReadOnlyObjectProperty<StatusText> algorithmStatusProperty() {
        return algorithmStatus;
    }

    ReadOnlyObjectProperty<StatusText> verifyResultProperty() {
        return verifyResult;
    }

    ReadOnlyObjectProperty<KeyMode> keyModeProperty() {
        return keyMode;
    }

    public ReadOnlyStringProperty decodeErrorProperty() {
        return decodeError;
    }

    public ReadOnlyStringProperty decodeWarningProperty() {
        return decodeWarning;
    }

    public BooleanProperty secretBase64Property() {
        return secretBase64;
    }

    public StringProperty publicKeyTextProperty() {
        return publicKeyText;
    }

    public ObjectProperty<Integer> clockSkewSecondsProperty() {
        return clockSkewSeconds;
    }

    public BooleanBinding canVerify() {
        return decoded.isNotNull().and(keyMode.isNotEqualTo(KeyMode.UNAVAILABLE));
    }

    /** Petunjuk key di card verifikasi, mengikuti {@code alg} token. */
    public StringBinding verifyHint() {
        return Bindings.createStringBinding(
                () -> switch (keyMode.get()) {
                    case SECRET -> Messages.format("devutils.jwt.verify.hint.secret", algorithmName());
                    case PUBLIC_KEY ->
                        Messages.format("devutils.jwt.verify.hint.publicKey", algorithmName(), expectedKeyName());
                    case UNAVAILABLE -> Messages.get("devutils.jwt.verify.hint.unavailable");
                },
                keyMode,
                decoded);
    }

    /** Secret dari {@code PasswordField}; array selalu di-wipe, termasuk bila verifikasi tidak bisa dijalankan. */
    public void verifySecret(char[] secret) {
        if (decoded.get() == null || keyMode.get() != KeyMode.SECRET) {
            Arrays.fill(secret, '\0');
            return;
        }
        verify(new KeyInput.Secret(secret, secretBase64.get()));
    }

    public void verifyPublicKey() {
        if (decoded.get() == null || keyMode.get() != KeyMode.PUBLIC_KEY) {
            return;
        }
        verify(new KeyInput.PublicKeyText(publicKeyText.get()));
    }

    public void dispose() {
        debounce.stop();
        cancelRunningTask();
        // Lepas referensi token & key dari property supaya tidak tertahan lebih lama dari tool-nya
        token.set("");
        publicKeyText.set("");
        clearDecoded();
    }

    private void decode() {
        cancelRunningTask();
        long current = ++generation;
        String text = token.get();
        if (text.isBlank()) {
            clearDecoded();
            return;
        }
        runningTask = context.tasks()
                .submit(
                        (progress, cancellation) -> decodeUseCase.execute(text),
                        jwt -> {
                            if (current == generation) {
                                show(jwt);
                            }
                        },
                        failure -> {
                            if (current == generation) {
                                showDecodeFailure(failure);
                            }
                        });
    }

    private void show(DecodedJwt jwt) {
        decoded.set(jwt);
        header.set(jwt.prettyHeader());
        payload.set(jwt.prettyPayload());
        signature.set(jwt.raw().signatureText());
        Instant now = clock.instant();
        claims.setAll(jwt.claims().stream().map(claim -> row(claim, now)).toList());
        decodeError.set("");
        formatStatus.set(new StatusText(Messages.get("devutils.jwt.badge.format.valid"), Tone.SUCCESS));
        refreshTimeStatus();
        applyAlgorithm(jwt);
    }

    private void applyAlgorithm(DecodedJwt jwt) {
        resetVerification();
        JwtAlgorithm algorithm = jwt.algorithm().orElse(null);
        if (algorithm == null) {
            keyMode.set(KeyMode.UNAVAILABLE);
            algorithmStatus.set(unknownAlgorithm());
            decodeWarning.set(
                    jwt.algorithmName().isEmpty()
                            ? Messages.get("devutils.jwt.error.noAlgorithm")
                            : Messages.format("devutils.jwt.error.unsupportedAlgorithm", jwt.algorithmName()));
            return;
        }
        algorithmStatus.set(
                new StatusText(Messages.format("devutils.jwt.badge.algorithm", algorithm.headerName()), Tone.INFO));
        if (algorithm == JwtAlgorithm.NONE) {
            // alg none tidak pernah valid (Security #20); verifikasi tidak ditawarkan sama sekali
            keyMode.set(KeyMode.UNAVAILABLE);
            decodeWarning.set(Messages.get("devutils.jwt.error.none"));
            signatureStatus.set(new StatusText(Messages.get("devutils.jwt.badge.signature.none"), Tone.DANGER));
            algorithmStatus.set(new StatusText(
                    Messages.format("devutils.jwt.badge.algorithm", algorithm.headerName()), Tone.DANGER));
            return;
        }
        decodeWarning.set("");
        keyMode.set(algorithm.family() == JwtAlgorithm.Family.HMAC ? KeyMode.SECRET : KeyMode.PUBLIC_KEY);
    }

    private void showDecodeFailure(Throwable failure) {
        clearDecoded();
        if (!(failure instanceof JwtToolException error)) {
            ErrorDialogs.showUnexpected(context.owner(), failure);
            return;
        }
        decodeError.set(JwtErrorMessages.describe(error));
        formatStatus.set(new StatusText(Messages.get("devutils.jwt.badge.format.invalid"), Tone.DANGER));
    }

    private void verify(KeyInput input) {
        try {
            SignatureStatus status = verifyUseCase.execute(decoded.get(), input);
            boolean valid = status == SignatureStatus.VALID;
            signatureStatus.set(valid ? validSignature() : invalidSignature());
            verifyResult.set(
                    valid
                            ? new StatusText(Messages.get("devutils.jwt.verify.valid"), Tone.SUCCESS)
                            : new StatusText(Messages.get("devutils.jwt.verify.invalid"), Tone.DANGER));
        } catch (JwtToolException error) {
            signatureStatus.set(new StatusText(Messages.get("devutils.jwt.badge.signature.rejected"), Tone.DANGER));
            verifyResult.set(new StatusText(JwtErrorMessages.describe(error), Tone.DANGER));
        }
    }

    private void refreshTimeStatus() {
        DecodedJwt jwt = decoded.get();
        if (jwt == null) {
            timeStatus.set(noTime());
            return;
        }
        Duration skew =
                Duration.ofSeconds(clockSkewSeconds.get() == null ? DEFAULT_SKEW_SECONDS : clockSkewSeconds.get());
        TimeStatus status = jwt.timeStatus(clock.instant(), skew);
        timeStatus.set(
                switch (status) {
                    case ACTIVE -> new StatusText(Messages.get("devutils.jwt.badge.time.active"), Tone.SUCCESS);
                    case EXPIRED -> new StatusText(Messages.get("devutils.jwt.badge.time.expired"), Tone.DANGER);
                    case NOT_YET_VALID ->
                        new StatusText(Messages.get("devutils.jwt.badge.time.notYetValid"), Tone.WARNING);
                    case NO_TIME_CLAIMS -> noTime();
                });
    }

    private ClaimRow row(Claim claim, Instant now) {
        String value = claim.instant()
                .map(instant -> JwtTimeFormat.describe(claim.name(), instant, now, zone))
                .orElse(claim.value());
        String explanation = claim.registered()
                ? Messages.get("devutils.jwt.claim." + claim.name())
                : Messages.get("devutils.jwt.claim.custom");
        return new ClaimRow(claim.name(), value, explanation);
    }

    private void resetVerification() {
        verifyResult.set(StatusText.EMPTY);
        DecodedJwt jwt = decoded.get();
        boolean unsecured =
                jwt != null && jwt.algorithm().filter(JwtAlgorithm.NONE::equals).isPresent();
        if (!unsecured) {
            signatureStatus.set(notVerified());
        }
    }

    private void clearDecoded() {
        decoded.set(null);
        header.set("");
        payload.set("");
        signature.set("");
        claims.clear();
        decodeError.set("");
        decodeWarning.set("");
        keyMode.set(KeyMode.UNAVAILABLE);
        formatStatus.set(noToken());
        timeStatus.set(noTime());
        signatureStatus.set(notVerified());
        algorithmStatus.set(unknownAlgorithm());
        verifyResult.set(StatusText.EMPTY);
    }

    private void cancelRunningTask() {
        if (runningTask != null) {
            runningTask.cancel();
            runningTask = null;
        }
    }

    /** Aman dipanggil saat {@code decoded} dan {@code keyMode} sedang berganti (binding dihitung di antaranya). */
    private String expectedKeyName() {
        DecodedJwt jwt = decoded.get();
        return jwt == null
                ? ""
                : jwt.algorithm().map(JwtErrorMessages::expectedKey).orElse("");
    }

    private String algorithmName() {
        DecodedJwt jwt = decoded.get();
        return jwt == null ? "" : jwt.algorithmName();
    }

    private static StatusText noToken() {
        return new StatusText(Messages.get("devutils.jwt.badge.format.empty"), Tone.NEUTRAL);
    }

    private static StatusText noTime() {
        return new StatusText(Messages.get("devutils.jwt.badge.time.none"), Tone.NEUTRAL);
    }

    private static StatusText notVerified() {
        return new StatusText(Messages.get("devutils.jwt.badge.signature.notVerified"), Tone.NEUTRAL);
    }

    private static StatusText validSignature() {
        return new StatusText(Messages.get("devutils.jwt.badge.signature.valid"), Tone.SUCCESS);
    }

    private static StatusText invalidSignature() {
        return new StatusText(Messages.get("devutils.jwt.badge.signature.invalid"), Tone.DANGER);
    }

    private static StatusText unknownAlgorithm() {
        return new StatusText(Messages.get("devutils.jwt.badge.algorithm.unknown"), Tone.NEUTRAL);
    }
}
