package io.github.lukmnh.toolbox.productivity.rename.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.productivity.rename.domain.Placement;
import io.github.lukmnh.toolbox.productivity.rename.domain.RemoveMode;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameRule;
import io.github.lukmnh.toolbox.productivity.shared.domain.CaseStyle;

/** Jenis aturan untuk menu "Tambah aturan". Urutan enum = urutan menu. */
enum RuleKind {
    REPLACE("productivity.rename.rule.replace"),
    AFFIX("productivity.rename.rule.affix"),
    NUMBERING("productivity.rename.rule.numbering"),
    CASE("productivity.rename.rule.case"),
    DATE("productivity.rename.rule.date"),
    REMOVE("productivity.rename.rule.remove"),
    EXTENSION("productivity.rename.rule.extension");

    private static final String DEFAULT_SEPARATOR = "_";
    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    private static final String DEFAULT_EXTENSION = "txt";
    private static final int DEFAULT_PADDING = 3;

    private final String labelKey;

    RuleKind(String labelKey) {
        this.labelKey = labelKey;
    }

    String label() {
        return Messages.get(labelKey);
    }

    RenameRule defaultRule() {
        return switch (this) {
            case REPLACE -> new RenameRule.Replace("", "", false, false, false);
            case AFFIX -> new RenameRule.Affix("", "");
            case NUMBERING -> new RenameRule.Numbering(1, 1, DEFAULT_PADDING, Placement.START, DEFAULT_SEPARATOR);
            case CASE -> new RenameRule.ChangeCase(CaseStyle.LOWER, false);
            case DATE -> new RenameRule.InsertDate(DEFAULT_DATE_PATTERN, Placement.START, DEFAULT_SEPARATOR);
            case REMOVE -> new RenameRule.Remove(RemoveMode.FIRST, 1, 1, 1);
            case EXTENSION -> new RenameRule.ChangeExtension(DEFAULT_EXTENSION);
        };
    }

    static RuleKind of(RenameRule rule) {
        return switch (rule) {
            case RenameRule.Replace replace -> REPLACE;
            case RenameRule.Affix affix -> AFFIX;
            case RenameRule.Numbering numbering -> NUMBERING;
            case RenameRule.ChangeCase changeCase -> CASE;
            case RenameRule.InsertDate date -> DATE;
            case RenameRule.Remove remove -> REMOVE;
            case RenameRule.ChangeExtension extension -> EXTENSION;
        };
    }
}
