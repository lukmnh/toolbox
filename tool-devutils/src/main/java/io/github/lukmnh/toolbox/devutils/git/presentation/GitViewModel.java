package io.github.lukmnh.toolbox.devutils.git.presentation;

import io.github.lukmnh.toolbox.common.tool.ToolContext;
import io.github.lukmnh.toolbox.common.ui.Notifier;
import io.github.lukmnh.toolbox.devutils.git.application.BuildGitCommandUseCase;
import io.github.lukmnh.toolbox.devutils.git.domain.GitBuild;
import io.github.lukmnh.toolbox.devutils.git.domain.GitCatalog;
import io.github.lukmnh.toolbox.devutils.git.domain.GitCheatsheet;
import io.github.lukmnh.toolbox.devutils.git.domain.GitCommandSpec;
import io.github.lukmnh.toolbox.devutils.git.domain.GitOption;
import io.github.lukmnh.toolbox.devutils.git.domain.GitRequest;
import io.github.lukmnh.toolbox.devutils.git.domain.GitSnippet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * State Git Builder. Ganti command → field argumen/opsi dibuat ulang (view ikut membangun ulang form). Setiap perubahan
 * field langsung membangun ulang command; murah dan sinkron, jadi tanpa debounce atau background task.
 */
final class GitViewModel {

    /** Default {@code commit}: command yang paling sering dicari. */
    private static final String DEFAULT_COMMAND = "commit";

    private final BuildGitCommandUseCase buildCommand;
    private final ToolContext context;
    private final ObjectProperty<GitCommandSpec> command = new SimpleObjectProperty<>(GitCatalog.COMMANDS.stream()
            .filter(spec -> spec.id().equals(DEFAULT_COMMAND))
            .findFirst()
            .orElseThrow());
    private final Map<String, StringProperty> arguments = new LinkedHashMap<>();
    private final Map<String, BooleanProperty> flags = new LinkedHashMap<>();
    private final Map<String, StringProperty> values = new LinkedHashMap<>();
    private final ObjectProperty<GitBuild> result = new SimpleObjectProperty<>();
    private final StringProperty query = new SimpleStringProperty("");

    GitViewModel(BuildGitCommandUseCase buildCommand, ToolContext context) {
        this.buildCommand = buildCommand;
        this.context = context;
        // Didaftarkan sebelum listener view: map sudah berisi field command baru saat view membangun ulang form
        command.addListener((observable, previous, current) -> resetFields());
        resetFields();
    }

    private void resetFields() {
        arguments.clear();
        flags.clear();
        values.clear();
        GitCommandSpec spec = command.get();
        spec.arguments().forEach(argument -> arguments.put(argument.key(), watched(new SimpleStringProperty(""))));
        for (GitOption option : spec.options()) {
            if (option.takesValue()) {
                values.put(option.key(), watched(new SimpleStringProperty("")));
            } else {
                flags.put(option.key(), watched(new SimpleBooleanProperty()));
            }
        }
        rebuild();
    }

    private <P extends Property<?>> P watched(P property) {
        property.addListener(observable -> rebuild());
        return property;
    }

    private void rebuild() {
        Set<String> enabled = flags.entrySet().stream()
                .filter(entry -> entry.getValue().get())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        result.set(buildCommand.execute(new GitRequest(command.get(), texts(arguments), enabled, texts(values))));
    }

    private static Map<String, String> texts(Map<String, StringProperty> fields) {
        return fields.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().get()));
    }

    /** Cheatsheet yang cocok dengan query di command atau teks penjelasannya. */
    List<GitSnippet> snippets() {
        String needle = query.get().strip().toLowerCase(Locale.ROOT);
        return GitCheatsheet.SNIPPETS.stream()
                .filter(snippet -> needle.isEmpty()
                        || snippet.command().toLowerCase(Locale.ROOT).contains(needle)
                        || GitTexts.snippet(snippet).toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    ObjectProperty<GitCommandSpec> commandProperty() {
        return command;
    }

    StringProperty argument(String key) {
        return arguments.get(key);
    }

    BooleanProperty flag(String key) {
        return flags.get(key);
    }

    StringProperty value(String key) {
        return values.get(key);
    }

    ReadOnlyObjectProperty<GitBuild> resultProperty() {
        return result;
    }

    StringProperty queryProperty() {
        return query;
    }

    Notifier notifier() {
        return context.notifier();
    }
}
