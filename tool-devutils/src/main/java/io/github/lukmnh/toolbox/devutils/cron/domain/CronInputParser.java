package io.github.lukmnh.toolbox.devutils.cron.domain;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Normalisasi input sebelum diserahkan ke engine: prefix zona Go, macro, jumlah field, dan karakter khusus yang tidak
 * didukung target. Cek ini sengaja di domain supaya pesan errornya spesifik per target, bukan pesan generik library.
 */
public final class CronInputParser {

    private static final String EVERY = "@every";
    private static final List<String> GO_ZONE_PREFIXES = List.of("CRON_TZ=", "TZ=");
    private static final String SECONDS_ZERO = "0 ";

    private CronInputParser() {}

    /** @throws InvalidCronException bila input tidak valid untuk target */
    public static ParsedCron parse(CronRequest request) {
        String text = request.expression().strip();
        ZoneId zone = request.zone();
        if (request.target() == CronTarget.GO) {
            for (String prefix : GO_ZONE_PREFIXES) {
                if (text.startsWith(prefix)) {
                    int end = firstWhitespace(text);
                    zone = zone(text.substring(prefix.length(), end));
                    text = text.substring(end).strip();
                }
            }
        }
        if (text.isEmpty()) {
            throw InvalidCronException.empty();
        }
        CronSpec spec = text.startsWith("@") ? macro(text, request) : fields(text, request);
        return new ParsedCron(spec, zone);
    }

    private static CronSpec macro(String text, CronRequest request) {
        if (request.target() == CronTarget.GO && text.toLowerCase(Locale.ROOT).startsWith(EVERY)) {
            return GoInterval.parse(text.substring(EVERY.length()).strip());
        }
        String word = firstWord(text);
        if (!word.equals(text) && CronMacro.of(word).isPresent()) {
            // "@daily 5": macro tidak boleh diikuti field lain
            throw InvalidCronException.invalidExpression();
        }
        CronMacro macro = CronMacro.of(text).orElseThrow(() -> InvalidCronException.unsupportedMacro(word));
        if (!request.target().supports(macro)) {
            throw InvalidCronException.unsupportedMacro(macro.text());
        }
        if (macro == CronMacro.REBOOT) {
            throw InvalidCronException.noSchedule(macro.text());
        }
        boolean seconds = request.target().hasSecondsField(request.goWithSeconds());
        return fields((seconds ? SECONDS_ZERO : "") + macro.fiveFields(), request);
    }

    private static CronFields fields(String text, CronRequest request) {
        CronTarget target = request.target();
        String[] values = text.split("\\s+");
        List<Integer> counts = target.fieldCounts(request.goWithSeconds());
        if (!counts.contains(values.length)) {
            throw InvalidCronException.fieldCount(counts, values.length);
        }
        List<CronFieldKind> layout = target.layout(values.length);
        List<String> normalized = new ArrayList<>(values.length);
        for (int index = 0; index < values.length; index++) {
            CronFieldKind kind = layout.get(index);
            for (CronSpecial special : CronSpecial.detect(kind, values[index])) {
                if (!target.supports(special)) {
                    throw InvalidCronException.unsupportedSpecial(kind, values[index]);
                }
            }
            boolean wildcard = values[index].equals("?") && target.questionMarkIsWildcard();
            normalized.add(wildcard ? "*" : values[index]);
        }
        return new CronFields(target, layout, normalized);
    }

    private static ZoneId zone(String id) {
        try {
            return ZoneId.of(id);
        } catch (DateTimeException e) {
            throw InvalidCronException.invalidTimeZone(id);
        }
    }

    private static int firstWhitespace(String text) {
        for (int index = 0; index < text.length(); index++) {
            if (Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }
        return text.length();
    }

    private static String firstWord(String text) {
        return text.substring(0, firstWhitespace(text));
    }
}
