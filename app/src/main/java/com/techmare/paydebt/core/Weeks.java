package com.techmare.paydebt.core;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/** Week maths. Weeks start on Monday. */
public final class Weeks {
    private Weeks() {}

    public static LocalDate startOfWeek(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate dateOf(long millis, ZoneId zone) {
        return Instant.ofEpochMilli(millis).atZone(zone).toLocalDate();
    }

    /** 0 = the week containing {@code today}, 1 = last week, and so on. Future dates give negative numbers. */
    public static int weeksAgo(long millis, LocalDate today, ZoneId zone) {
        LocalDate entryWeek = startOfWeek(dateOf(millis, zone));
        LocalDate thisWeek = startOfWeek(today);
        return (int) ChronoUnit.WEEKS.between(entryWeek, thisWeek);
    }

    public static boolean isThisWeek(long millis, LocalDate today, ZoneId zone) {
        return weeksAgo(millis, today, zone) == 0;
    }
}
