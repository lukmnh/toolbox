package io.github.lukmnh.toolbox.devutils.cron.domain;

/** Arti ekspresi: jadwal per field, atau interval tetap ({@code @every} Go). */
public sealed interface CronMeaning permits CronSchedule, GoInterval {}
