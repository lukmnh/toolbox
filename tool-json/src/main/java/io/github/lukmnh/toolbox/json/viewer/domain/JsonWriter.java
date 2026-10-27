package io.github.lukmnh.toolbox.json.viewer.domain;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Menulis {@link JsonValue} sebagai teks JSON (ringkas atau pretty dengan indent 2 spasi). */
public final class JsonWriter {

    public static final String ELLIPSIS = "…";

    private static final String INDENT = "  ";
    private static final int NO_LIMIT = Integer.MAX_VALUE;
    private static final int LAST_CONTROL_CHAR = 0x1F;

    private JsonWriter() {}

    public static String compact(JsonValue value) {
        StringBuilder out = new StringBuilder();
        write(out, value, false, 0, NO_LIMIT);
        return out.toString();
    }

    public static String pretty(JsonValue value) {
        StringBuilder out = new StringBuilder();
        write(out, value, true, 0, NO_LIMIT);
        return out.toString();
    }

    /** JSON ringkas maksimal {@code maxChars} karakter; berhenti menulis lebih awal untuk nilai besar. */
    public static String preview(JsonValue value, int maxChars) {
        StringBuilder out = new StringBuilder();
        write(out, value, false, 0, maxChars + 1);
        return truncate(out.toString(), maxChars);
    }

    public static String truncate(String text, int maxChars) {
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + ELLIPSIS;
    }

    public static void quote(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c <= LAST_CONTROL_CHAR) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    private static void write(StringBuilder out, JsonValue value, boolean pretty, int depth, int limit) {
        if (out.length() >= limit) {
            return;
        }
        switch (value) {
            case JsonValue.JsonObject object -> writeObject(out, object.fields(), pretty, depth, limit);
            case JsonValue.JsonArray array -> writeArray(out, array.items(), pretty, depth, limit);
            case JsonValue.JsonString string -> quote(out, string.value());
            case JsonValue.JsonNumber number -> out.append(number.text());
            case JsonValue.JsonBoolean bool -> out.append(bool.value());
            case JsonValue.JsonNull ignored -> out.append("null");
        }
    }

    private static void writeObject(
            StringBuilder out, Map<String, JsonValue> fields, boolean pretty, int depth, int limit) {
        if (fields.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append('{');
        Iterator<Map.Entry<String, JsonValue>> entries = fields.entrySet().iterator();
        while (entries.hasNext() && out.length() < limit) {
            Map.Entry<String, JsonValue> entry = entries.next();
            newLine(out, pretty, depth + 1);
            quote(out, entry.getKey());
            out.append(pretty ? ": " : ":");
            write(out, entry.getValue(), pretty, depth + 1, limit);
            if (entries.hasNext()) {
                out.append(',');
            }
        }
        newLine(out, pretty, depth);
        out.append('}');
    }

    private static void writeArray(StringBuilder out, List<JsonValue> items, boolean pretty, int depth, int limit) {
        if (items.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append('[');
        for (int i = 0; i < items.size() && out.length() < limit; i++) {
            newLine(out, pretty, depth + 1);
            write(out, items.get(i), pretty, depth + 1, limit);
            if (i < items.size() - 1) {
                out.append(',');
            }
        }
        newLine(out, pretty, depth);
        out.append(']');
    }

    private static void newLine(StringBuilder out, boolean pretty, int depth) {
        if (pretty) {
            out.append('\n').append(INDENT.repeat(depth));
        }
    }
}
