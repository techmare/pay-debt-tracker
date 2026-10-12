package com.techmare.paydebt;

import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.techmare.paydebt.core.Budget;
import com.techmare.paydebt.core.Fmt;
import com.techmare.paydebt.core.Payoff;
import com.techmare.paydebt.core.Streak;
import com.techmare.paydebt.core.Weeks;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds the four tabs. */
final class Screens {
    private Screens() {}

    private static final int HISTORY_LIMIT = 300;
    private static final int WHAT_IF_EXTRA = 100;

    private static TextView title(MainActivity a, String s) {
        return Ui.text(a, s, 26, Ui.TEXT, true);
    }

    private static LinearLayout.LayoutParams space(MainActivity a, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        lp.setMargins(0, Ui.dp(a, topDp), 0, 0);
        return lp;
    }

    // ---------- Home ----------

    static void home(MainActivity a, LinearLayout out) {
        final Data d = a.data;
        final LocalDate today = LocalDate.now();
        final ZoneId zone = ZoneId.systemDefault();

        out.addView(title(a, "Pay & Debt"));
        TextView date = Ui.text(a, Fmt.date(today), 14, Ui.MUTED, false);
        date.setPadding(0, 0, 0, Ui.dp(a, 14));
        out.addView(date);

        if (d.debts.isEmpty()) {
            LinearLayout c = Ui.card(a);
            c.addView(Ui.text(a, "Welcome", 18, Ui.TEXT, true));
            TextView t = Ui.text(a, "Add what you owe and how much you will pay each week. "
                    + "The app shows when you will be debt-free and how much you can safely spend.", 14, Ui.MUTED, false);
            t.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 12));
            c.addView(t);
            c.addView(Ui.button(a, "Set up", Ui.GREEN, v -> Dialogs.setup(a)));
            out.addView(c);
        }

        long remaining = d.totalRemaining();
        long target = d.settings.weeklyDebtTarget;
        long paidWeek = d.sumThisWeek(Data.Entry.DEBT, today, zone);

        // You owe
        if (!d.debts.isEmpty()) {
            LinearLayout c = Ui.card(a);
            c.addView(Ui.text(a, "You owe", 14, Ui.MUTED, false));
            c.addView(Ui.text(a, Fmt.dh(remaining), 38, remaining > 0 ? Ui.TEXT : Ui.GREEN, true));
            int pct = Payoff.percentPaid(d.totalStart(), remaining);
            c.addView(Ui.bar(a, pct, Ui.GREEN));
            c.addView(Ui.text(a, remaining > 0
                    ? pct + "% paid back of " + Fmt.dh(d.totalStart())
                    : "You are debt free.", 13, Ui.MUTED, false));
            Data.Debt focus = d.focusDebt();
            if (focus != null && d.debts.size() > 1) {
                TextView f = Ui.text(a, "Pay off next: " + focus.name + " (" + Fmt.dh(d.balance(focus)) + ")",
                        14, Ui.TEXT, false);
                f.setPadding(0, Ui.dp(a, 8), 0, 0);
                c.addView(f);
            }
            out.addView(c);
        }

        // Payoff date
        if (remaining > 0) {
            LinearLayout c = Ui.card(a);
            int weeks = Payoff.weeksToPayoff(remaining, target);
            if (weeks < 0) {
                c.addView(Ui.text(a, "Payoff date", 14, Ui.MUTED, false));
                TextView t = Ui.text(a, "Set a weekly debt payment in Settings to see when you will be done.",
                        14, Ui.TEXT, false);
                t.setPadding(0, Ui.dp(a, 4), 0, 0);
                c.addView(t);
            } else {
                c.addView(Ui.text(a, "Debt-free around", 14, Ui.MUTED, false));
                c.addView(Ui.text(a, Fmt.date(Payoff.payoffDate(today, remaining, target)), 22, Ui.TEXT, true));
                TextView t = Ui.text(a, Fmt.weeks(weeks) + " at " + Fmt.dh(target) + " a week", 14, Ui.MUTED, false);
                t.setPadding(0, Ui.dp(a, 2), 0, 0);
                c.addView(t);
                int saved = Payoff.weeksSaved(remaining, target, WHAT_IF_EXTRA);
                if (saved > 0) {
                    TextView w = Ui.text(a, "Pay " + Fmt.dh(WHAT_IF_EXTRA) + " more a week and finish "
                            + Fmt.weeks(saved) + " sooner.", 14, Ui.BLUE, false);
                    w.setPadding(0, Ui.dp(a, 8), 0, 0);
                    c.addView(w);
                }
            }
            out.addView(c);
        }

        // Free to spend
        {
            long due = Budget.debtDueThisWeek(target, remaining, paidWeek);
            long reserved = Budget.reserved(due, paidWeek);
            long free = Budget.freeCash(d.settings.startingCash, d.sum(Data.Entry.PAY),
                    d.sum(Data.Entry.DEBT), d.sum(Data.Entry.SPEND), due, paidWeek);
            LinearLayout c = Ui.card(a);
            c.addView(Ui.text(a, "Free to spend", 14, Ui.MUTED, false));
            c.addView(Ui.text(a, Fmt.dh(free), 34, free >= 0 ? Ui.GREEN : Ui.RED, true));
            String detail = "Set aside for debt this week: " + Fmt.dh(reserved)
                    + "\nDebt paid this week: " + Fmt.dh(paidWeek) + " of " + Fmt.dh(due)
                    + "\nSpent this week: " + Fmt.dh(d.sumThisWeek(Data.Entry.SPEND, today, zone));
            TextView t = Ui.text(a, detail, 14, Ui.MUTED, false);
            t.setPadding(0, Ui.dp(a, 6), 0, 0);
            c.addView(t);
            if (free < 0) {
                TextView w = Ui.text(a, "You have spent more than you have. Check History for mistakes.",
                        14, Ui.RED, false);
                w.setPadding(0, Ui.dp(a, 6), 0, 0);
                c.addView(w);
            }
            out.addView(c);
        }

        // Quick actions
        out.addView(Ui.button(a, "+ Got paid", Ui.GREEN, v -> Dialogs.gotPaid(a)), space(a, 0));
        LinearLayout row = Ui.hbox(a);
        View paid = Ui.button(a, "Paid debt", Ui.BLUE, v -> Dialogs.paidDebt(a));
        View spent = Ui.button(a, "Spent", Ui.AMBER, v -> Dialogs.spent(a));
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, Ui.WRAP, 1f);
        left.setMargins(0, 0, Ui.dp(a, 6), 0);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, Ui.WRAP, 1f);
        right.setMargins(Ui.dp(a, 6), 0, 0, 0);
        row.addView(paid, left);
        row.addView(spent, right);
        out.addView(row, space(a, 10));

        // Streak
        if (target > 0 && !d.debts.isEmpty()) {
            int tracked = d.weeksTracked(today, zone);
            int streak = Streak.onTimeWeeks(d.debtPaidPerWeek(today, zone, tracked), tracked, target);
            if (streak > 0) {
                TextView s = Ui.text(a, Fmt.weeks(streak) + " in a row with the debt payment on time",
                        14, Ui.GREEN, true);
                s.setPadding(0, Ui.dp(a, 16), 0, 0);
                out.addView(s);
            }
        }
    }

    // ---------- History ----------

    static void history(MainActivity a, LinearLayout out) {
        final Data d = a.data;
        final ZoneId zone = ZoneId.systemDefault();
        out.addView(title(a, "History"));
        TextView hint = Ui.text(a, "Tap an entry to delete it.", 14, Ui.MUTED, false);
        hint.setPadding(0, 0, 0, Ui.dp(a, 14));
        out.addView(hint);

        List<Data.Entry> all = d.newestFirst();
        if (all.isEmpty()) {
            LinearLayout c = Ui.card(a);
            c.addView(Ui.text(a, "Nothing here yet. Use Got paid, Paid debt or Spent on the Home tab.", 14, Ui.MUTED, false));
            out.addView(c);
            return;
        }

        int shown = 0;
        for (final Data.Entry e : all) {
            if (shown++ >= HISTORY_LIMIT) break;
            String name;
            String sign;
            int color;
            if (e.type == Data.Entry.PAY) {
                name = "Got paid" + (e.source.isEmpty() ? "" : " (" + e.source + ")");
                sign = "+";
                color = Ui.GREEN;
            } else if (e.type == Data.Entry.DEBT) {
                Data.Debt debt = d.debt(e.debtId);
                name = "Paid " + (debt == null ? "a deleted debt" : debt.name);
                sign = "-";
                color = Ui.BLUE;
            } else {
                name = e.note.isEmpty() ? "Spent" : "Spent: " + e.note;
                sign = "-";
                color = Ui.AMBER;
            }
            final String when = Fmt.shortDate(Weeks.dateOf(e.time, zone));
            final String amount = sign + Fmt.dh(e.amount);
            final String description = name + "\n" + when + "\n" + amount;

            LinearLayout c = Ui.card(a);
            LinearLayout line = Ui.hbox(a);
            LinearLayout left = Ui.vbox(a);
            left.addView(Ui.text(a, name, 16, Ui.TEXT, true));
            left.addView(Ui.text(a, when, 13, Ui.MUTED, false));
            line.addView(left, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
            line.addView(Ui.text(a, amount, 17, color, true));
            c.addView(line);
            c.setOnClickListener(v -> Dialogs.deleteEntry(a, e, description));
            out.addView(c);
        }
        if (all.size() > HISTORY_LIMIT) {
            out.addView(Ui.text(a, "Showing the newest " + HISTORY_LIMIT + " of " + all.size() + " entries.",
                    13, Ui.MUTED, false));
        }
    }

    // ---------- Debts ----------

    static void debts(MainActivity a, LinearLayout out) {
        final Data d = a.data;
        out.addView(title(a, "Debts"));
        TextView hint = Ui.text(a, "With several debts, finish the smallest one first.", 14, Ui.MUTED, false);
        hint.setPadding(0, 0, 0, Ui.dp(a, 14));
        out.addView(hint);

        Data.Debt focus = d.focusDebt();
        for (final Data.Debt x : d.debts) {
            long bal = d.balance(x);
            LinearLayout c = Ui.card(a);
            c.addView(Ui.text(a, x.name, 18, Ui.TEXT, true));
            c.addView(Ui.text(a, bal > 0 ? Fmt.dh(bal) + " left" : "Paid off", 22, bal > 0 ? Ui.TEXT : Ui.GREEN, true));
            c.addView(Ui.bar(a, Payoff.percentPaid(x.start, bal), Ui.GREEN));
            c.addView(Ui.text(a, "Started at " + Fmt.dh(x.start), 13, Ui.MUTED, false));
            if (x == focus && d.debts.size() > 1) {
                TextView f = Ui.text(a, "Pay this one first", 13, Ui.GREEN, true);
                f.setPadding(0, Ui.dp(a, 4), 0, 0);
                c.addView(f);
            }
            LinearLayout row = Ui.hbox(a);
            row.setPadding(0, Ui.dp(a, 10), 0, 0);
            row.addView(Ui.chip(a, "Edit", v -> Dialogs.debtForm(a, x)));
            row.addView(Ui.chip(a, "Delete", v -> Dialogs.deleteDebt(a, x)));
            c.addView(row);
            out.addView(c);
        }
        out.addView(Ui.button(a, "+ Add debt", Ui.GREEN, v -> Dialogs.debtForm(a, null)), space(a, 4));
    }

    // ---------- Settings ----------

    private static Spinner spinner(MainActivity a, List<String> items, int selected) {
        Spinner sp = new Spinner(a);
        ArrayAdapter<String> ad = new ArrayAdapter<>(a, android.R.layout.simple_spinner_item, items);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        sp.setSelection(selected);
        return sp;
    }

    private static TextView label(MainActivity a, String s) {
        TextView t = Ui.text(a, s, 13, Ui.MUTED, false);
        t.setPadding(0, Ui.dp(a, 12), 0, 0);
        return t;
    }

    private static EditText field(MainActivity a, LinearLayout c, String label, long value) {
        c.addView(label(a, label));
        EditText e = Ui.numberField(a, "0");
        if (value > 0) e.setText(String.valueOf(value));
        c.addView(e);
        return e;
    }

    static void settings(MainActivity a, LinearLayout out) {
        final Data.Settings s = a.data.settings;
        out.addView(title(a, "Settings"));
        TextView gap = Ui.text(a, "", 8, Ui.MUTED, false);
        out.addView(gap);

        LinearLayout plan = Ui.card(a);
        plan.addView(Ui.text(a, "Your plan", 18, Ui.TEXT, true));
        final EditText target = field(a, plan, "Debt payment each week", s.weeklyDebtTarget);
        final EditText pay = field(a, plan, "Pay each week (rough)", s.weeklyPay);
        final EditText cash = field(a, plan, "Cash you had when you started", s.startingCash);
        out.addView(plan);

        LinearLayout rem = Ui.card(a);
        rem.addView(Ui.text(a, "Weekly reminder", 18, Ui.TEXT, true));
        final Switch on = new Switch(a);
        on.setText("Remind me on pay day");
        on.setChecked(s.reminderOn);
        on.setPadding(0, Ui.dp(a, 8), 0, 0);
        rem.addView(on);
        rem.addView(label(a, "Day"));
        List<String> days = new ArrayList<>();
        for (int i = 1; i <= 7; i++) days.add(DayOfWeek.of(i).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        final Spinner day = spinner(a, days, s.reminderDay - 1);
        rem.addView(day);
        rem.addView(label(a, "Time"));
        List<String> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) hours.add(String.format(Locale.ENGLISH, "%02d:00", h));
        final Spinner hour = spinner(a, hours, s.reminderHour);
        rem.addView(hour);
        out.addView(rem);

        out.addView(Ui.button(a, "Save", Ui.GREEN, v -> {
            String t = target.getText().toString().trim();
            String p = pay.getText().toString().trim();
            String c = cash.getText().toString().trim();
            long tv = t.isEmpty() ? 0 : Fmt.parseAmount(t);
            long pv = p.isEmpty() ? 0 : Fmt.parseAmount(p);
            long cv = c.isEmpty() ? 0 : Fmt.parseAmount(c);
            if (tv < 0 || pv < 0 || cv < 0) {
                Toast.makeText(a, "Use whole numbers only", Toast.LENGTH_SHORT).show();
                return;
            }
            s.weeklyDebtTarget = tv;
            s.weeklyPay = pv;
            s.startingCash = cv;
            s.reminderOn = on.isChecked();
            s.reminderDay = day.getSelectedItemPosition() + 1;
            s.reminderHour = hour.getSelectedItemPosition();
            if (s.reminderOn) a.askNotificationPermission();
            a.changed();
            Toast.makeText(a, "Saved", Toast.LENGTH_SHORT).show();
        }), space(a, 0));

        LinearLayout backup = Ui.card(a);
        backup.setLayoutParams(space(a, 16));
        backup.addView(Ui.text(a, "Backup", 18, Ui.TEXT, true));
        TextView note = Ui.text(a, "Your data lives only on this phone. If you lose the phone you lose the data, "
                + "so share a backup to yourself now and then.", 14, Ui.MUTED, false);
        note.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 10));
        backup.addView(note);
        LinearLayout row = Ui.hbox(a);
        row.addView(Ui.chip(a, "Share backup", v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "Pay & Debt backup");
            i.putExtra(Intent.EXTRA_TEXT, Store.toJson(a.data, true));
            a.startActivity(Intent.createChooser(i, "Save backup"));
        }));
        row.addView(Ui.chip(a, "Restore", v -> Dialogs.restore(a)));
        row.addView(Ui.chip(a, "Erase all", v -> Dialogs.eraseAll(a)));
        backup.addView(row);
        out.addView(backup);

        TextView version = Ui.text(a, "Pay & Debt 1.0", 12, Ui.MUTED, false);
        version.setPadding(0, Ui.dp(a, 8), 0, 0);
        out.addView(version);
    }
}
