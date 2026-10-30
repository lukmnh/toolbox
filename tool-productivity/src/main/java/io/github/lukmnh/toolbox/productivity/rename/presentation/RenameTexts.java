package io.github.lukmnh.toolbox.productivity.rename.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.common.ui.Tone;
import io.github.lukmnh.toolbox.productivity.rename.domain.FileOrder;
import io.github.lukmnh.toolbox.productivity.rename.domain.NameIssue;
import io.github.lukmnh.toolbox.productivity.rename.domain.NameValidator;
import io.github.lukmnh.toolbox.productivity.rename.domain.Placement;
import io.github.lukmnh.toolbox.productivity.rename.domain.RemoveMode;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameRule;
import io.github.lukmnh.toolbox.productivity.rename.domain.RenameStatus;
import io.github.lukmnh.toolbox.productivity.shared.domain.CaseStyle;

/** Label enum dan ringkasan aturan untuk UI. */
final class RenameTexts {

    private static final String PREFIX = "productivity.rename.";

    private RenameTexts() {}

    static String caseStyle(CaseStyle style) {
        return Messages.get(PREFIX + "case." + style.name());
    }

    static String placement(Placement placement) {
        return Messages.get(PREFIX + "placement." + placement.name());
    }

    static String removeMode(RemoveMode mode) {
        return Messages.get(PREFIX + "removeMode." + mode.name());
    }

    static String order(FileOrder order) {
        return Messages.get(PREFIX + "order." + order.name());
    }

    static String status(RenameStatus status) {
        return Messages.get(PREFIX + "status." + status.name());
    }

    static String issue(NameIssue issue) {
        return switch (issue) {
            case TOO_LONG ->
                Messages.format(PREFIX + "issue.TOO_LONG", NameValidator.MAX_CHARACTERS, NameValidator.MAX_UTF8_BYTES);
            default -> Messages.get(PREFIX + "issue." + issue.name());
        };
    }

    static Tone tone(RenameStatus status) {
        return switch (status) {
            case OK -> Tone.SUCCESS;
            case UNCHANGED -> Tone.NEUTRAL;
            case CONFLICT -> Tone.WARNING;
            case INVALID -> Tone.DANGER;
        };
    }

    /** Ringkasan satu baris di daftar aturan, misalnya {@code Cari & ganti: "a" → "b"}. */
    static String summary(RenameRule rule) {
        String label = RuleKind.of(rule).label();
        return Messages.format(PREFIX + "rule.summary", label, detail(rule));
    }

    private static String detail(RenameRule rule) {
        return switch (rule) {
            case RenameRule.Replace replace ->
                Messages.format(PREFIX + "summary.replace", replace.find(), replace.replacement());
            case RenameRule.Affix affix -> Messages.format(PREFIX + "summary.affix", affix.prefix(), affix.suffix());
            case RenameRule.Numbering numbering ->
                Messages.format(PREFIX + "summary.numbering", numbering.format(0), placement(numbering.placement()));
            case RenameRule.ChangeCase changeCase -> caseStyle(changeCase.style());
            case RenameRule.InsertDate date ->
                Messages.format(PREFIX + "summary.date", date.pattern(), placement(date.placement()));
            case RenameRule.Remove remove -> removeDetail(remove);
            case RenameRule.ChangeExtension extension ->
                extension.extension().isEmpty()
                        ? Messages.get(PREFIX + "summary.extension.remove")
                        : Messages.format(PREFIX + "summary.extension", extension.extension());
        };
    }

    private static String removeDetail(RenameRule.Remove remove) {
        return switch (remove.mode()) {
            case FIRST -> Messages.format(PREFIX + "summary.remove.FIRST", remove.count());
            case LAST -> Messages.format(PREFIX + "summary.remove.LAST", remove.count());
            case RANGE -> Messages.format(PREFIX + "summary.remove.RANGE", remove.from(), remove.to());
        };
    }
}
