package io.github.lukmnh.toolbox.devutils.cron.presentation;

import io.github.lukmnh.toolbox.devutils.cron.application.BuildCronUseCase;
import io.github.lukmnh.toolbox.devutils.cron.application.ExplainCronUseCase;
import java.util.Objects;

/** Kumpulan use case Cron Builder & Explainer, dirakit di composition root. */
public record CronUseCases(ExplainCronUseCase explain, BuildCronUseCase build) {

    public CronUseCases {
        Objects.requireNonNull(explain, "explain");
        Objects.requireNonNull(build, "build");
    }
}
