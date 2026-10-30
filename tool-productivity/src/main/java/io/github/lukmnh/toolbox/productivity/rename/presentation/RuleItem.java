package io.github.lukmnh.toolbox.productivity.rename.presentation;

import io.github.lukmnh.toolbox.productivity.rename.domain.RenameRule;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

/** Satu aturan di daftar: record aturan (immutable, diganti utuh oleh editor) + status on/off. */
final class RuleItem {

    private final BooleanProperty enabled = new SimpleBooleanProperty(true);
    private final ObjectProperty<RenameRule> rule;

    RuleItem(RenameRule rule) {
        this.rule = new SimpleObjectProperty<>(rule);
    }

    BooleanProperty enabledProperty() {
        return enabled;
    }

    ObjectProperty<RenameRule> ruleProperty() {
        return rule;
    }

    boolean enabled() {
        return enabled.get();
    }

    RenameRule rule() {
        return rule.get();
    }
}
