package io.github.lukmnh.toolbox.json.viewer.domain;

import io.github.lukmnh.toolbox.json.shared.domain.InvalidQueryException;
import io.github.lukmnh.toolbox.json.shared.domain.InvalidQueryException.Reason;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Path ke field di dalam record: {@code a.b.c}, {@code items[0].id}. */
public record FieldPath(String text, List<Segment> segments) {

    public sealed interface Segment {
        record Key(String name) implements Segment {}

        record Index(int index) implements Segment {}
    }

    public FieldPath {
        segments = List.copyOf(segments);
    }

    /**
     * @param position posisi {@code text} di dalam query, untuk pesan error
     * @throws InvalidQueryException bila format path salah
     */
    public static FieldPath parse(String text, int position) {
        List<Segment> segments = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '[') {
                i = parseIndex(text, i, position, segments);
            } else if (c == '.' && !segments.isEmpty()) {
                // Setelah titik wajib ada nama key: "a..b" dan "a.[0]" ditolak
                i = parseKey(text, i + 1, position, segments);
            } else if (segments.isEmpty()) {
                i = parseKey(text, i, position, segments);
            } else {
                // Key menempel tanpa titik, misalnya "a[0]b"
                throw new InvalidQueryException(Reason.INVALID_PATH, position + i, text);
            }
        }
        if (segments.isEmpty()) {
            throw new InvalidQueryException(Reason.INVALID_PATH, position, text);
        }
        return new FieldPath(text, segments);
    }

    public Optional<JsonValue> resolve(JsonValue root) {
        JsonValue current = root;
        for (Segment segment : segments) {
            current = step(current, segment);
            if (current == null) {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }

    /** @return anak sesuai segment, atau {@code null} bila tidak ada */
    private static JsonValue step(JsonValue value, Segment segment) {
        return switch (segment) {
            case Segment.Key key
            when value instanceof JsonValue.JsonObject object -> object.fields().get(key.name());
            case Segment.Index index
            when value instanceof JsonValue.JsonArray array ->
                index.index() < array.items().size() ? array.items().get(index.index()) : null;
            default -> null;
        };
    }

    private static int parseKey(String text, int start, int position, List<Segment> segments) {
        int end = start;
        while (end < text.length() && text.charAt(end) != '.' && text.charAt(end) != '[') {
            if (text.charAt(end) == ']') {
                throw new InvalidQueryException(Reason.INVALID_PATH, position + end, text);
            }
            end++;
        }
        if (end == start) {
            throw new InvalidQueryException(Reason.INVALID_PATH, position + start, text);
        }
        segments.add(new Segment.Key(text.substring(start, end)));
        return end;
    }

    private static int parseIndex(String text, int open, int position, List<Segment> segments) {
        int close = text.indexOf(']', open);
        if (close < 0 || close == open + 1) {
            throw new InvalidQueryException(Reason.INVALID_PATH, position + open, text);
        }
        String digits = text.substring(open + 1, close);
        if (!digits.chars().allMatch(Character::isDigit)) {
            throw new InvalidQueryException(Reason.INVALID_PATH, position + open, text);
        }
        try {
            segments.add(new Segment.Index(Integer.parseInt(digits)));
        } catch (NumberFormatException e) {
            throw new InvalidQueryException(Reason.INVALID_PATH, position + open, text);
        }
        return close + 1;
    }
}
