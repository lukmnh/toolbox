package io.github.lukmnh.toolbox.devutils.encoder.presentation;

import io.github.lukmnh.toolbox.devutils.encoder.domain.QueryParam;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Baris tabel query parameter yang bisa diedit langsung di sel. */
final class ParamRow {

    private final StringProperty key;
    private final StringProperty value;

    ParamRow(String key, String value) {
        this.key = new SimpleStringProperty(key);
        this.value = new SimpleStringProperty(value);
    }

    static ParamRow of(QueryParam param) {
        return new ParamRow(param.key(), param.value());
    }

    StringProperty keyProperty() {
        return key;
    }

    StringProperty valueProperty() {
        return value;
    }

    QueryParam toParam() {
        return new QueryParam(nullToEmpty(key.get()), nullToEmpty(value.get()));
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
