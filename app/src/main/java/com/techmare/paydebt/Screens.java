package com.techmare.paydebt;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;

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

/** Builds the four tabs with Material 3 components. */
final class Screens {
    private Screens() {}

    private static final int HISTORY_LIMIT = 300;
    private static final int WHAT_IF_EXTRA = 100;

    private static void header(MainActivity a, LinearLayout out, String title, String subtitle) {
        out.addView(Ui.text(a, title, Ui.HEADLINE_MEDIUM, Ui.ON_SURFACE));
        TextView sub = Ui.text(a, subtitle, Ui.BODY_MEDIUM, Ui.ON_SURFACE_VARIANT);
        sub.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 20));
        out.addView(sub);
    }

    private static TextView line(MainActivity a, LinearLayout parent, String s, int appearance, int colorAttr, int topDp) {
        TextView t = Ui.text(a, s, appearance, colorAttr);
        Ui.add(parent, t, topDp);
        return t;
    }

    // ---------- Home ----------

    static void home(MainActivity a, LinearLayout out) {
        final Data d = a.data;
        final LocalDate today = LocalDate.now();
        final ZoneId zone = ZoneId.systemDefault();

        header(a, out, "Pay & Debt", Fmt.date(today));

        long remaining = d.totalRemaining();
        long target = d.settings.weeklyDebtTarget;
        long paidWeek = d.sumThisWeek(Data.Entry.DEBT, today, zone);

        if (d.debts.isEmpty()) {
            MaterialCardView card = Ui.card(a, Ui.PRIMARY_CONTAINER);
            LinearLayout c = Ui.inner(card);
            c.addView(Ui.text(a, "Welcome", Ui.TITLE_LARGE, Ui.ON_PRIMARY_CONTAINER));
            line(a, c, "Add what you owe and how much you will pay each week. "
                    + "The app shows when you will be debt-free and how much you can safely spend.",
                    Ui.BODY_MEDIUM, Ui.ON_PRIMARY_CONTAINER, 8);
            Ui.add(c, Ui.filled(a, "Set up", 0, v -> Dialogs.setup(a)), 16);
            out.addView(card);
        } else {
            // You owe
            MaterialCardView card = Ui.card(a, Ui.PRIMARY_CONTAINER);
            LinearLayout c = Ui.inner(card);
            c.addView(Ui.text(a, "You owe", Ui.LABEL_LARGE, Ui.ON_PRIMARY_CONTAINER));
            c.addView(Ui.text(a, Fmt.dh(remaining), Ui.DISPLAY_MEDIUM, Ui.ON_PRIMARY_CONTAINER));
            int pct = Payoff.percentPaid(d.totalStart(), remaining);
            c.addView(Ui.progress(a, pct, Ui.color(a, Ui.PRIMARY), Ui.color(a, Ui.SECONDARY_CONTAINER)));
            c.addView(Ui.text(a, remaining > 0
                    ? pct + "% paid back of " + Fmt.dh(d.totalStart())
                    : "You are debt free.", Ui.BODY_MEDIUM, Ui.ON_PRIMARY_CONTAINER));
            Data.Debt focus = d.focusDebt();
            if (focus != null && d.debts.size() > 1) {
                line(a, c, "Pay off next: " + focus.name + " (" + Fmt.dh(d.balance(focus)) + ")",
                        Ui.BODY_MEDIUM, Ui.ON_PRIMARY_CONTAINER, 8);
            }
            out.addView(card);
        }

        // Payoff date
        if (remaining > 0) {
            MaterialCardView card = Ui.card(a, Ui.CONTAINER_HIGH);
            LinearLayout c = Ui.inner(card);
            int weeks = Payoff.weeksToPayoff(remaining, target);
            if (weeks < 0) {
                c.addView(Ui.text(a, "Payoff date", Ui.LABEL_LARGE, Ui.ON_SURFACE_VARIANT));
                line(a, c, "Set a weekly debt payment in Settings to see when you will be done.",
                        Ui.BODY_MEDIUM, Ui.ON_SURFACE, 4);
            } else {
                c.addView(Ui.text(a, "Debt-free around", Ui.LABEL_LARGE, Ui.ON_SURFACE_VARIANT));
                c.addView(Ui.text(a, Fmt.date(Payoff.payoffDate(today, remaining, target)),
                        Ui.HEADLINE_SMALL, Ui.ON_SURFACE));
                line(a, c, Fmt.weeks(weeks) + " at " + Fmt.dh(target) + " a week",
                        Ui.BODY_MEDIUM, Ui.ON_SURFACE_VARIANT, 2);
                int saved = Payoff.weeksSaved(remaining, target, WHAT_IF_EXTRA);
                if (saved > 0) {
                    line(a, c, "Pay " + Fmt.dh(WHAT_IF_EXTRA) + " more a week and finish "
                            + Fmt.weeks(saved) + " sooner.", Ui.BODY_MEDIUM, Ui.PRIMARY, 12);
                }
            }
            out.addView(card);
        }

        // Free to spend
        {
            long due = Budget.debtDueThisWeek(target, remaining, paidWeek);
            long reserved = Budget.reserved(due, paidWeek);
            long free = Budget.freeCash(d.settings.startingCash, d.sum(Data.Entry.PAY),
                    d.sum(Data.Entry.DEBT), d.sum(Data.Entry.SPEND), due, paidWeek);
            boolean ok = free >= 0;
            int container = ok ? Ui.SECONDARY_CONTAINER : Ui.ERROR_CONTAINER;
            int onContainer = ok ? Ui.ON_SECONDARY_CONTAINER : Ui.ON_ERROR_CONTAINER;
            MaterialCardView card = Ui.card(a, container);
            LinearLayout c = Ui.inner(card);
            c.addView(Ui.text(a, "Free to spend", Ui.LABEL_LARGE, onContainer));
            c.addView(Ui.text(a, Fmt.dh(free), Ui.DISPLAY_SMALL, onContainer));
            line(a, c, "Set aside for debt this week: " + Fmt.dh(reserved)
                    + "\nDebt paid this week: " + Fmt.dh(paidWeek) + " of " + Fmt.dh(due)
                    + "\nSpent this week: " + Fmt.dh(d.sumThisWeek(Data.Entry.SPEND, today, zone)),
                    Ui.BODY_MEDIUM, onContainer, 8);
            if (!ok) {
                line(a, c, "You have spent more than you have. Check History for mistakes.",
                        Ui.BODY_MEDIUM, onContainer, 8);
            }
            out.addView(card);
        }

        // Quick actions
        Ui.add(out, Ui.filled(a, "Got paid", R.drawable.ic_add, v -> Dialogs.gotPaid(a)), 4);
        LinearLayout row = Ui.hbox(a);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, Ui.WRAP, 1f);
        left.setMarginEnd(Ui.dp(a, 6));
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, Ui.WRAP, 1f);
        right.setMarginStart(Ui.dp(a, 6));
        row.addView(Ui.tonal(a, "Paid debt", R.drawable.ic_check, v -> Dialogs.paidDebt(a)), left);
        row.addView(Ui.outlined(a, "Spent", R.drawable.ic_remove, v -> Dialogs.spent(a)), right);
        Ui.add(out, row, 10);

        // Streak
        if (target > 0 && !d.debts.isEmpty()) {
            int tracked = d.weeksTracked(today, zone);
            int streak = Streak.onTimeWeeks(d.debtPaidPerWeek(today, zone, tracked), tracked, target);
            if (streak > 0) {
                Chip chip = new Chip(a);
                chip.setText(Fmt.weeks(streak) + " in a row on time");
                chip.setChipIconResource(R.drawable.ic_flame);
                chip.setClickable(false);
                chip.setCheckable(false);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP);
                lp.topMargin = Ui.dp(a, 16);
                out.addView(chip, lp);
            }
        }
    }

    // ---------- History ----------

    static void history(MainActivity a, LinearLayout out) {
        final Data d = a.data;
        final ZoneId zone = ZoneId.systemDefault();
        header(a, out, "History", "Tap an entry to delete it.");

        List<Data.Entry> all = d.newestFirst();
        if (all.isEmpty()) {
            MaterialCardView card = Ui.card(a, Ui.CONTAINER_HIGH);
            Ui.inner(card).addView(Ui.text(a, "Nothing here yet. Use Got paid, Paid debt or Spent on the Home tab.",
                    Ui.BODY_MEDIUM, Ui.ON_SURFACE_VARIANT));
            out.addView(card);
            return;
        }

        MaterialCardView card = Ui.card(a, Ui.CONTAINER_LOW);
        LinearLayout list = Ui.inner(card);
        list.setPadding(Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 8));

        int shown = 0;
        for (final Data.Entry e : all) {
            if (shown++ >= HISTORY_LIMIT) break;
            String name;
            String sign;
            int icon;
            int container;
            int onContainer;
            int amountColor;
            if (e.type == Data.Entry.PAY) {
                name = "Got paid" + (e.source.isEmpty() ? "" : " (" + e.source + ")");
                sign = "+";
                icon = R.drawable.ic_add;
                container = Ui.PRIMARY_CONTAINER;
                onContainer = Ui.ON_PRIMARY_CONTAINER;
                amountColor = Ui.PRIMARY;
            } else if (e.type == Data.Entry.DEBT) {
                Data.Debt debt = d.debt(e.debtId);
                name = "Paid " + (debt == null ? "a deleted debt" : debt.name);
                sign = "-";
                icon = R.drawable.ic_check;
                container = Ui.TERTIARY_CONTAINER;
                onContainer = Ui.ON_TERTIARY_CONTAINER;
                amountColor = Ui.ON_SURFACE;
            } else {
                name = e.note.isEmpty() ? "Spent" : "Spent: " + e.note;
                sign = "-";
                icon = R.drawable.ic_remove;
                container = Ui.SECONDARY_CONTAINER;
                onContainer = Ui.ON_SECONDARY_CONTAINER;
                amountColor = Ui.ON_SURFACE;
            }
            final String when = Fmt.shortDate(Weeks.dateOf(e.time, zone));
            final String amount = sign + Fmt.dh(e.amount);
            final String description = name + "\n" + when + "\n" + amount;

            LinearLayout rowView = Ui.hbox(a);
            rowView.setGravity(android.view.Gravity.CENTER_VERTICAL);
            rowView.setPadding(Ui.dp(a, 12), Ui.dp(a, 10), Ui.dp(a, 12), Ui.dp(a, 10));
            rowView.addView(Ui.badge(a, icon, container, onContainer));
            LinearLayout texts = Ui.vbox(a);
            texts.addView(Ui.text(a, name, Ui.BODY_LARGE, Ui.ON_SURFACE));
            texts.addView(Ui.text(a, when, Ui.BODY_SMALL, Ui.ON_SURFACE_VARIANT));
            rowView.addView(texts, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
            rowView.addView(Ui.text(a, amount, Ui.TITLE_MEDIUM, amountColor));
            rowView.setBackground(clickableBackground(a));
            rowView.setOnClickListener(v -> Dialogs.deleteEntry(a, e, description));
            list.addView(rowView, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        }
        out.addView(card);
        if (all.size() > HISTORY_LIMIT) {
            out.addView(Ui.text(a, "Showing the newest " + HISTORY_LIMIT + " of " + all.size() + " entries.",
                    Ui.BODY_SMALL, Ui.ON_SURFACE_VARIANT));
        }
    }

    /** Standard ripple used for tappable rows. */
    private static android.graphics.drawable.Drawable clickableBackground(MainActivity a) {
        android.util.TypedValue tv = new android.util.TypedValue();
        a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        return a.getDrawable(tv.resourceId);
    }

    // ---------- Debts ----------

    static void debts(MainActivity a, LinearLayout out) {
        final Data d = a.data;
        header(a, out, "Debts", "With several debts, finish the smallest one first.");

        Data.Debt focus = d.focusDebt();
        for (final Data.Debt x : d.debts) {
            long bal = d.balance(x);
            boolean first = x == focus && d.debts.size() > 1;
            MaterialCardView card = Ui.card(a, first ? Ui.PRIMARY_CONTAINER : Ui.CONTAINER_HIGH);
            int on = first ? Ui.ON_PRIMARY_CONTAINER : Ui.ON_SURFACE;
            int onVar = first ? Ui.ON_PRIMARY_CONTAINER : Ui.ON_SURFACE_VARIANT;
            LinearLayout c = Ui.inner(card);
            c.addView(Ui.text(a, x.name, Ui.TITLE_LARGE, on));
            c.addView(Ui.text(a, bal > 0 ? Fmt.dh(bal) + " left" : "Paid off", Ui.HEADLINE_SMALL, on));
            c.addView(Ui.progress(a, Payoff.percentPaid(x.start, bal), Ui.color(a, Ui.PRIMARY),
                    Ui.color(a, first ? Ui.SECONDARY_CONTAINER : Ui.CONTAINER_LOW)));
            c.addView(Ui.text(a, "Started at " + Fmt.dh(x.start)
                    + (first ? "  •  Pay this one first" : ""), Ui.BODY_MEDIUM, onVar));
            LinearLayout row = Ui.hbox(a);
            row.setGravity(android.view.Gravity.END);
            row.addView(Ui.textButton(a, "Edit", v -> Dialogs.debtForm(a, x)));
            row.addView(Ui.textButton(a, "Delete", v -> Dialogs.deleteDebt(a, x)));
            Ui.add(c, row, 8);
            out.addView(card);
        }
        Ui.add(out, Ui.filled(a, "Add debt", R.drawable.ic_add, v -> Dialogs.debtForm(a, null)), 4);
    }

    // ---------- Settings ----------

    private static EditText field(MainActivity a, LinearLayout c, String label, long value) {
        EditText e = Ui.input(a, c, label, Ui.NUMBER, "dh");
        if (value > 0) e.setText(String.valueOf(value));
        return e;
    }

    static void settings(MainActivity a, LinearLayout out) {
        final Data.Settings s = a.data.settings;
        header(a, out, "Settings", "Your plan and reminder");

        MaterialCardView planCard = Ui.card(a, Ui.CONTAINER_LOW);
        LinearLayout plan = Ui.inner(planCard);
        plan.addView(Ui.text(a, "Your plan", Ui.TITLE_LARGE, Ui.ON_SURFACE));
        final EditText target = field(a, plan, "Debt payment each week", s.weeklyDebtTarget);
        final EditText pay = field(a, plan, "Pay each week (rough)", s.weeklyPay);
        final EditText cash = field(a, plan, "Cash you had when you started", s.startingCash);
        out.addView(planCard);

        MaterialCardView remCard = Ui.card(a, Ui.CONTAINER_LOW);
        LinearLayout rem = Ui.inner(remCard);
        rem.addView(Ui.text(a, "Weekly reminder", Ui.TITLE_LARGE, Ui.ON_SURFACE));
        final MaterialSwitch on = new MaterialSwitch(a);
        on.setText("Remind me on pay day");
        on.setChecked(s.reminderOn);
        Ui.add(rem, on, 8);
        List<String> days = new ArrayList<>();
        for (int i = 1; i <= 7; i++) days.add(DayOfWeek.of(i).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        final Ui.Dropdown day = Ui.dropdown(a, "Day", days, s.reminderDay - 1);
        Ui.add(rem, day.layout, 12);
        List<String> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) hours.add(String.format(Locale.ENGLISH, "%02d:00", h));
        final Ui.Dropdown hour = Ui.dropdown(a, "Time", hours, s.reminderHour);
        Ui.add(rem, hour.layout, 12);
        out.addView(remCard);

        out.addView(Ui.filled(a, "Save", 0, v -> {
            long tv = parse(target);
            long pv = parse(pay);
            long cv = parse(cash);
            if (tv < 0 || pv < 0 || cv < 0) {
                a.snack("Use whole numbers only");
                return;
            }
            s.weeklyDebtTarget = tv;
            s.weeklyPay = pv;
            s.startingCash = cv;
            s.reminderOn = on.isChecked();
            s.reminderDay = day.index + 1;
            s.reminderHour = hour.index;
            if (s.reminderOn) a.askNotificationPermission();
            a.changed();
            a.snack("Saved");
        }));

        MaterialCardView backupCard = Ui.card(a, Ui.CONTAINER_LOW);
        ((LinearLayout.LayoutParams) backupCard.getLayoutParams()).topMargin = Ui.dp(a, 20);
        LinearLayout backup = Ui.inner(backupCard);
        backup.addView(Ui.text(a, "Backup", Ui.TITLE_LARGE, Ui.ON_SURFACE));
        line(a, backup, "Your data lives only on this phone. If you lose the phone you lose the data, "
                + "so share a backup to yourself now and then.", Ui.BODY_MEDIUM, Ui.ON_SURFACE_VARIANT, 4);
        LinearLayout row = Ui.hbox(a);
        row.addView(Ui.textButton(a, "Share backup", v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "Pay & Debt backup");
            i.putExtra(Intent.EXTRA_TEXT, Store.toJson(a.data, true));
            a.startActivity(Intent.createChooser(i, "Save backup"));
        }));
        row.addView(Ui.textButton(a, "Restore", v -> Dialogs.restore(a)));
        Ui.add(backup, row, 8);
        LinearLayout row2 = Ui.hbox(a);
        View erase = Ui.textButton(a, "Erase all", v -> Dialogs.eraseAll(a));
        ((com.google.android.material.button.MaterialButton) erase)
                .setTextColor(Ui.color(a, Ui.ERROR));
        row2.addView(erase);
        backup.addView(row2);
        out.addView(backupCard);

        out.addView(Ui.text(a, "Pay & Debt 2.0", Ui.BODY_SMALL, Ui.ON_SURFACE_VARIANT));
    }

    /** Empty counts as 0; anything that is not a whole number gives -1. */
    private static long parse(EditText e) {
        String t = e.getText().toString().trim();
        return t.isEmpty() ? 0 : Fmt.parseAmount(t);
    }
}
