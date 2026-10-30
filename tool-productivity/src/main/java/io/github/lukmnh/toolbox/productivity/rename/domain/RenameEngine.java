package io.github.lukmnh.toolbox.productivity.rename.domain;

import io.github.lukmnh.toolbox.productivity.shared.domain.CaseConverter;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Menerapkan aturan berurutan ke satu nama file. Regex dan pola tanggal dikompilasi sekali di constructor, sehingga
 * aturan yang salah langsung ditolak sebelum file mana pun diproses.
 */
public final class RenameEngine {

    /** Nama bulan/hari mengikuti bahasa UI. */
    private static final Locale DATE_LOCALE = Locale.forLanguageTag("id-ID");

    private final List<RenameRule> rules;
    private final PatternReplacer replacer;
    private final ZoneId zone;
    private final Map<RenameRule.Replace, CompiledReplace> replacements = new HashMap<>();
    private final Map<RenameRule.InsertDate, DateTimeFormatter> formatters = new HashMap<>();

    /** @throws InvalidRuleException bila regex atau pola tanggal tidak valid */
    public RenameEngine(List<RenameRule> rules, PatternReplacer replacer, ZoneId zone) {
        this.rules = List.copyOf(rules);
        this.replacer = replacer;
        this.zone = zone;
        for (RenameRule rule : this.rules) {
            if (rule instanceof RenameRule.Replace replace && !replace.find().isEmpty()) {
                replacements.put(replace, compile(replace));
            }
            if (rule instanceof RenameRule.InsertDate date) {
                formatters.put(date, formatter(date.pattern()));
            }
        }
    }

    /** @param index posisi file di batch (mulai 0), untuk penomoran */
    public String newName(FileEntry file, int index) {
        NameParts parts = NameParts.of(file.name());
        for (RenameRule rule : rules) {
            parts = apply(rule, parts, file, index);
        }
        return parts.fullName();
    }

    private NameParts apply(RenameRule rule, NameParts parts, FileEntry file, int index) {
        return switch (rule) {
            case RenameRule.Replace replace -> replace(replace, parts);
            case RenameRule.Affix affix -> parts.withStem(affix.prefix() + parts.stem() + affix.suffix());
            case RenameRule.Numbering numbering ->
                parts.withStem(
                        numbering.placement().insert(parts.stem(), numbering.format(index), numbering.separator()));
            case RenameRule.ChangeCase changeCase -> changeCase(changeCase, parts);
            case RenameRule.InsertDate date ->
                parts.withStem(date.placement().insert(parts.stem(), formatDate(date, file), date.separator()));
            case RenameRule.Remove remove -> parts.withStem(remove.removeFrom(parts.stem()));
            case RenameRule.ChangeExtension extension -> parts.withExtension(extension.extension());
        };
    }

    private NameParts replace(RenameRule.Replace rule, NameParts parts) {
        CompiledReplace compiled = replacements.get(rule);
        if (compiled == null) {
            return parts;
        }
        if (rule.includeExtension()) {
            return NameParts.of(replacer.replaceAll(compiled.pattern(), parts.fullName(), compiled.replacement()));
        }
        return parts.withStem(replacer.replaceAll(compiled.pattern(), parts.stem(), compiled.replacement()));
    }

    private static NameParts changeCase(RenameRule.ChangeCase rule, NameParts parts) {
        String stem = CaseConverter.convert(parts.stem(), rule.style(), false);
        String extension = rule.includeExtension()
                ? CaseConverter.convert(parts.extension(), rule.style(), false)
                : parts.extension();
        return new NameParts(stem, extension);
    }

    private String formatDate(RenameRule.InsertDate rule, FileEntry file) {
        try {
            return formatters.get(rule).format(file.lastModified().atZone(zone));
        } catch (DateTimeException e) {
            throw new InvalidRuleException(InvalidRuleException.Problem.INVALID_DATE_PATTERN, e);
        }
    }

    private static CompiledReplace compile(RenameRule.Replace rule) {
        int flags = rule.regex() ? 0 : Pattern.LITERAL;
        if (!rule.caseSensitive()) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        try {
            Pattern pattern = Pattern.compile(rule.find(), flags);
            // Mode teks biasa: $ dan \ di teks pengganti harus literal, bukan referensi grup
            String replacement = rule.regex() ? rule.replacement() : Matcher.quoteReplacement(rule.replacement());
            return new CompiledReplace(pattern, replacement);
        } catch (PatternSyntaxException e) {
            throw new InvalidRuleException(InvalidRuleException.Problem.INVALID_REGEX, e);
        }
    }

    private static DateTimeFormatter formatter(String pattern) {
        if (pattern.isBlank()) {
            throw new InvalidRuleException(InvalidRuleException.Problem.INVALID_DATE_PATTERN, null);
        }
        try {
            return DateTimeFormatter.ofPattern(pattern, DATE_LOCALE);
        } catch (IllegalArgumentException e) {
            throw new InvalidRuleException(InvalidRuleException.Problem.INVALID_DATE_PATTERN, e);
        }
    }

    private record CompiledReplace(Pattern pattern, String replacement) {}
}
