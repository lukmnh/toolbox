package io.github.lukmnh.toolbox.json.viewer.domain;

/** Sumber record untuk export: byte mentah (disalin apa adanya) dan parse bila perlu. */
public interface RecordSource {

    byte[] raw(int record);

    JsonValue parse(byte[] raw);
}
