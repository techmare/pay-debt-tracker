package com.techmare.paydebt;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.techmare.paydebt.core.Budget;
import com.techmare.paydebt.core.Fmt;

import java.time.LocalDate;
import java.time.ZoneId;

/** Shows the weekly pay-day notification. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Data d = Store.load(c);
        if (d.debts.isEmpty() || d.totalRemaining() <= 0) return;

        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();
        long paid = d.sumThisWeek(Data.Entry.DEBT, today, zone);
        long due = Budget.debtDueThisWeek(d.settings.weeklyDebtTarget, d.totalRemaining(), paid);
        long reserved = Budget.reserved(due, paid);

        String msg;
        if (reserved > 0) {
            msg = "Pay-day check: log your pay and send " + Fmt.dh(reserved) + " to your debt first.";
        } else {
            msg = "This week's debt payment is done. Log anything you spent.";
        }

        Reminders.createChannel(c);
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 2, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(c, Reminders.CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Pay & Debt")
                .setContentText(msg)
                .setStyle(new Notification.BigTextStyle().bigText(msg))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(1, n);
    }
}
