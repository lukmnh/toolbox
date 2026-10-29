package io.github.lukmnh.toolbox.devutils.cron.domain;

/** Input yang sudah dinormalisasi: field cron, atau interval {@code @every} Go. */
public sealed interface CronSpec permits CronFields, GoInterval {}
