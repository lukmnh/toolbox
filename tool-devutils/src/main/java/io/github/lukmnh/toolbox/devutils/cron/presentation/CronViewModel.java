package io.github.lukmnh.toolbox.devutils.cron.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.task.RunningTask;
import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.ClipboardActions;
import io.github.lukmnh.toolbox.common.ui.Debounce;
import io.github.lukmnh.toolbox.common.ui.ErrorDialogs;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronExplanation;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronPreset;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronRequest;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronTarget;
import io.github.lukmnh.toolbox.devutils.cron.domain.InvalidCronException;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

/**
 * State Cron Builder & Explainer. Builder menulis ke ekspresi; setiap perubahan ekspresi, target, opsi detik Go, atau
 * zona memicu penjelasan ulang (debounce 200 ms) di background. Hasil lama dibatalkan supaya tidak menimpa hasil baru.
 */
final class CronViewModel {

    /** Keputusan project: default Asia/Jakarta, bukan zona sistem. */
    static final String DEFAULT_ZONE = "Asia/Jakarta";

    static final String EXAMPLE_EXPRESSION = "*/15 8-17 * * 1-5";
    static final int DEFAULT_EVERY_MINUTES = 15;
    static final int DEFAULT_HOUR = 8;
    private static final Duration DEBOUNCE = Duration.millis(200);

    private final CronUseCases useCases;
    private final ToolContext context;
    private final Debounce debounce = new Debounce(DEBOUNCE, this::explain);

    private final ObjectProperty<CronTarget> target = new SimpleObjectProperty<>(CronTarget.UNIX);
    private final BooleanProperty goWithSeconds = new SimpleBooleanProperty();
    private final StringProperty zone = new SimpleStringProperty(DEFAULT_ZONE);
    private final StringProperty expression = new SimpleStringProperty(EXAMPLE_EXPRESSION);

    private final ObjectProperty<PresetKind> preset = new SimpleObjectProperty<>(PresetKind.WEEKDAYS);
    private final ObjectProperty<Integer> everyMinutes = new SimpleObjectProperty<>(DEFAULT_EVERY_MINUTES);
    private final ObjectProperty<Integer> hour = new SimpleObjectProperty<>(DEFAULT_HOUR);
    private final ObjectProperty<Integer> minute = new SimpleObjectProperty<>(0);
    private final ObjectProperty<DayOfWeek> dayOfWeek = new SimpleObjectProperty<>(DayOfWeek.MONDAY);
    private final ObjectProperty<Integer> dayOfMonth = new SimpleObjectProperty<>(1);
    private final List<StringProperty> customFields = List.of(
            new SimpleStringProperty("0"),
            new SimpleStringProperty("9-17"),
            new SimpleStringProperty("*"),
            new SimpleStringProperty("*"),
            new SimpleStringProperty("1-5"));

    private final ObjectProperty<CronExplanation> explanation = new SimpleObjectProperty<>();
    private final StringProperty error = new SimpleStringProperty("");
    private RunningTask runningTask;
    /** Ekspresi terakhir berasal dari Builder: ganti target → dibangun ulang dalam format target baru. */
    private boolean fromBuilder;

    private boolean applyingPreset;

    CronViewModel(CronUseCases useCases, ToolContext context) {
        this.useCases = useCases;
        this.context = context;
        expression.addListener((observable, previous, current) -> fromBuilder = applyingPreset);
        for (Observable source : List.<Observable>of(target, goWithSeconds)) {
            source.addListener(observable -> {
                if (fromBuilder) {
                    applyPreset();
                }
            });
        }
        List<Observable> builderInputs =
                new ArrayList<>(List.<Observable>of(preset, everyMinutes, hour, minute, dayOfWeek, dayOfMonth));
        builderInputs.addAll(customFields);
        builderInputs.forEach(source -> source.addListener(observable -> applyPreset()));
        for (Observable source : List.<Observable>of(target, goWithSeconds, zone, expression)) {
            source.addListener(observable -> debounce.trigger());
        }
        explain();
    }

    ObjectProperty<CronTarget> targetProperty() {
        return target;
    }

    BooleanProperty goWithSecondsProperty() {
        return goWithSeconds;
    }

    StringProperty zoneProperty() {
        return zone;
    }

    StringProperty expressionProperty() {
        return expression;
    }

    ObjectProperty<PresetKind> presetProperty() {
        return preset;
    }

    ObjectProperty<Integer> everyMinutesProperty() {
        return everyMinutes;
    }

    ObjectProperty<Integer> hourProperty() {
        return hour;
    }

    ObjectProperty<Integer> minuteProperty() {
        return minute;
    }

    ObjectProperty<DayOfWeek> dayOfWeekProperty() {
        return dayOfWeek;
    }

    ObjectProperty<Integer> dayOfMonthProperty() {
        return dayOfMonth;
    }

    /** Menit, jam, tanggal, bulan, hari-minggu untuk preset Kustom. */
    List<StringProperty> customFields() {
        return customFields;
    }

    ReadOnlyObjectProperty<CronExplanation> explanationProperty() {
        return explanation;
    }

    ReadOnlyStringProperty errorProperty() {
        return error;
    }

    void copyExpression() {
        ClipboardActions.copy(expression.get().strip(), context.notifier());
    }

    void copySnippet() {
        if (explanation.get() != null) {
            ClipboardActions.copy(explanation.get().snippet(), context.notifier());
        }
    }

    void dispose() {
        debounce.stop();
        cancelRunning();
    }

    private void applyPreset() {
        applyingPreset = true;
        try {
            expression.set(useCases.build().execute(currentPreset(), target.get(), goWithSeconds.get()));
        } finally {
            applyingPreset = false;
        }
    }

    private CronPreset currentPreset() {
        LocalTime time = LocalTime.of(hour.get(), minute.get());
        return switch (preset.get()) {
            case EVERY_MINUTES -> new CronPreset.EveryMinutes(everyMinutes.get());
            case HOURLY -> new CronPreset.Hourly(minute.get());
            case DAILY -> new CronPreset.Daily(time);
            case WEEKDAYS -> new CronPreset.Weekdays(time);
            case WEEKLY -> new CronPreset.Weekly(dayOfWeek.get(), time);
            case MONTHLY -> new CronPreset.Monthly(dayOfMonth.get(), time);
            case CUSTOM ->
                new CronPreset.Custom(
                        customFields.get(0).get(),
                        customFields.get(1).get(),
                        customFields.get(2).get(),
                        customFields.get(3).get(),
                        customFields.get(4).get());
        };
    }

    private void explain() {
        cancelRunning();
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(zone.get().strip());
        } catch (DateTimeException e) {
            explanation.set(null);
            error.set(Messages.format("devutils.cron.error.timeZone", zone.get().strip()));
            return;
        }
        CronRequest request = new CronRequest(expression.get(), target.get(), goWithSeconds.get(), zoneId);
        runningTask = context.tasks()
                .submit(
                        (progress, cancellation) -> useCases.explain().execute(request),
                        this::show,
                        failure -> fail(failure, request.target()));
    }

    private void show(CronExplanation result) {
        runningTask = null;
        error.set("");
        explanation.set(result);
    }

    /** Ekspresi tidak valid adalah keadaan normal saat mengetik: tampil inline, bukan dialog. */
    private void fail(Throwable failure, CronTarget requestTarget) {
        runningTask = null;
        explanation.set(null);
        if (failure instanceof InvalidCronException invalid) {
            error.set(CronErrorMessages.describe(invalid, requestTarget));
            return;
        }
        error.set("");
        ErrorDialogs.showUnexpected(context.owner(), failure);
    }

    private void cancelRunning() {
        if (runningTask != null) {
            runningTask.cancel();
            runningTask = null;
        }
    }
}
