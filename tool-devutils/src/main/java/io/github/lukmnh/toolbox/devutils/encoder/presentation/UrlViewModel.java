package io.github.lukmnh.toolbox.devutils.encoder.presentation;

import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.devutils.encoder.domain.InvalidPercentEncodingException;
import io.github.lukmnh.toolbox.devutils.encoder.domain.ParsedUrl;
import io.github.lukmnh.toolbox.devutils.encoder.domain.QueryParam;
import io.github.lukmnh.toolbox.devutils.encoder.domain.SpaceEncoding;
import io.github.lukmnh.toolbox.devutils.encoder.domain.UrlCodec;
import io.github.lukmnh.toolbox.devutils.encoder.domain.UrlParser;
import io.github.lukmnh.toolbox.devutils.shared.presentation.ClipboardActions;
import io.github.lukmnh.toolbox.devutils.shared.presentation.Debounce;
import java.util.Comparator;
import java.util.List;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

/** State dan aksi tab URL: encode/decode live dan parser URL dengan tabel query yang bisa diedit. */
public final class UrlViewModel {

    private static final Duration DEBOUNCE_DELAY = Duration.millis(200);

    private final Notifier notifier;

    private final ObjectProperty<UrlMode> mode = new SimpleObjectProperty<>(UrlMode.COMPONENT);
    private final ObjectProperty<Direction> direction = new SimpleObjectProperty<>(Direction.ENCODE);
    private final ObjectProperty<SpaceEncoding> spaces = new SimpleObjectProperty<>(SpaceEncoding.PERCENT_20);
    private final StringProperty input = new SimpleStringProperty("");
    private final StringProperty output = new SimpleStringProperty("");
    private final StringProperty error = new SimpleStringProperty("");
    private final Debounce convertDebounce = new Debounce(DEBOUNCE_DELAY, this::convert);

    private final StringProperty urlInput = new SimpleStringProperty("");
    private final ObjectProperty<ParsedUrl> parsed = new SimpleObjectProperty<>();
    /** Extractor membuat edit sel (key/value) ikut memicu rebuild URL. */
    private final ObservableList<ParamRow> params =
            FXCollections.observableArrayList(row -> new Observable[] {row.keyProperty(), row.valueProperty()});

    private final StringProperty rebuilt = new SimpleStringProperty("");
    private final StringProperty parseError = new SimpleStringProperty("");
    private final Debounce parseDebounce = new Debounce(DEBOUNCE_DELAY, this::parse);
    /** Saat tabel diisi ulang dari hasil parse, rebuild cukup sekali di akhir. */
    private boolean loadingParams;

    public UrlViewModel(Notifier notifier) {
        this.notifier = notifier;
        ChangeListener<Object> convertLater = (observable, previous, current) -> convertDebounce.trigger();
        input.addListener(convertLater);
        mode.addListener(convertLater);
        direction.addListener(convertLater);
        spaces.addListener(convertLater);
        spaces.addListener((observable, previous, current) -> rebuild());
        urlInput.addListener((observable, previous, current) -> parseDebounce.trigger());
        params.addListener((Observable observable) -> {
            if (!loadingParams) {
                rebuild();
            }
        });
    }

    // ---------- Encode / Decode ----------

    ObjectProperty<UrlMode> modeProperty() {
        return mode;
    }

    ObjectProperty<Direction> directionProperty() {
        return direction;
    }

    public ObjectProperty<SpaceEncoding> spacesProperty() {
        return spaces;
    }

    public StringProperty inputProperty() {
        return input;
    }

    public ReadOnlyStringProperty outputProperty() {
        return output;
    }

    public ReadOnlyStringProperty errorProperty() {
        return error;
    }

    public void swap() {
        if (output.get().isEmpty() || !error.get().isEmpty()) {
            return;
        }
        String previousOutput = output.get();
        direction.set(direction.get().opposite());
        input.set(previousOutput);
    }

    public void copyOutput() {
        ClipboardActions.copy(output.get(), notifier);
    }

    private void convert() {
        try {
            output.set(converted(input.get()));
            error.set("");
        } catch (InvalidPercentEncodingException e) {
            output.set("");
            error.set(EncoderErrorMessages.describe(e));
        }
    }

    private String converted(String text) {
        boolean encode = direction.get() == Direction.ENCODE;
        return switch (mode.get()) {
            case COMPONENT ->
                encode
                        ? UrlCodec.encodeComponent(text, spaces.get())
                        : UrlCodec.decodeComponent(text, spaces.get() == SpaceEncoding.PLUS);
            // '+' di URL lengkap (misalnya path) adalah karakter literal, bukan spasi
            case FULL_URL -> encode ? UrlCodec.encodeFullUrl(text) : UrlCodec.decodeComponent(text, false);
        };
    }

    // ---------- Parser URL ----------

    public StringProperty urlInputProperty() {
        return urlInput;
    }

    public ReadOnlyObjectProperty<ParsedUrl> parsedProperty() {
        return parsed;
    }

    ObservableList<ParamRow> params() {
        return params;
    }

    public ReadOnlyStringProperty rebuiltProperty() {
        return rebuilt;
    }

    public ReadOnlyStringProperty parseErrorProperty() {
        return parseError;
    }

    public void addParam() {
        params.add(new ParamRow("", ""));
    }

    public void removeParam(int index) {
        if (index >= 0 && index < params.size()) {
            params.remove(index);
        }
    }

    /** @return indeks baru baris yang dipindah, untuk menjaga seleksi tabel */
    public int moveParam(int index, int offset) {
        int target = index + offset;
        if (index < 0 || index >= params.size() || target < 0 || target >= params.size()) {
            return index;
        }
        ParamRow row = params.remove(index);
        params.add(target, row);
        return target;
    }

    /** Urut stabil berdasarkan key, jadi urutan key duplikat tetap. */
    public void sortParams() {
        FXCollections.sort(params, Comparator.comparing(row -> row.toParam().key()));
    }

    public void copyRebuilt() {
        ClipboardActions.copy(rebuilt.get(), notifier);
    }

    private void parse() {
        String text = urlInput.get();
        if (text.isBlank()) {
            showParsed(null, List.of(), "");
            return;
        }
        try {
            ParsedUrl url = UrlParser.parse(text);
            showParsed(url, url.params(), "");
        } catch (InvalidPercentEncodingException e) {
            showParsed(null, List.of(), EncoderErrorMessages.describe(e));
        }
    }

    private void showParsed(ParsedUrl url, List<QueryParam> queryParams, String message) {
        parsed.set(url);
        parseError.set(message);
        loadingParams = true;
        try {
            params.setAll(queryParams.stream().map(ParamRow::of).toList());
        } finally {
            loadingParams = false;
        }
        rebuild();
    }

    private void rebuild() {
        ParsedUrl url = parsed.get();
        rebuilt.set(
                url == null
                        ? ""
                        : url.toUrl(params.stream().map(ParamRow::toParam).toList(), spaces.get()));
    }

    public void dispose() {
        convertDebounce.stop();
        parseDebounce.stop();
    }
}
