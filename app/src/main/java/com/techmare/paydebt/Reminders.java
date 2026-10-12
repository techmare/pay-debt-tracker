package com.techmare.paydebt;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/** One repeating weekly reminder, set by the user in Settings. */
final class Reminders {
    static final String CHANNEL = "reminders";

    private Reminders() {}

    private static PendingIntent pending(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        return PendingIntent.getBroadcast(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void createChannel(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Pay-day reminder", NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(ch);
    }

    /** Cancels the old alarm and sets a new one if the reminder is on. */
    static void schedule(Context c, Data.Settings s) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pending(c);
        am.cancel(pi);
        if (!s.reminderOn) return;

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(s.reminderDay)))
                .withHour(s.reminderHour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        if (!next.isAfter(now)) next = next.plusWeeks(1);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(),
                7 * AlarmManager.INTERVAL_DAY, pi);
    }
}
