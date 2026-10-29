package io.github.lukmnh.toolbox.devutils.cron.application;

import io.github.lukmnh.toolbox.devutils.cron.domain.CronBuilder;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronPreset;
import io.github.lukmnh.toolbox.devutils.cron.domain.CronTarget;

/** Builder: preset → ekspresi sesuai format target. */
public final class BuildCronUseCase {

    public String execute(CronPreset preset, CronTarget target, boolean goWithSeconds) {
        return CronBuilder.build(preset, target, goWithSeconds);
    }
}
