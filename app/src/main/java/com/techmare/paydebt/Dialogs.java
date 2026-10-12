package com.techmare.paydebt;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.techmare.paydebt.core.Budget;
import com.techmare.paydebt.core.Fmt;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Every pop-up form in the app, as Material 3 dialogs. */
final class Dialogs {
    private Dialogs() {}

    // ---------- small helpers ----------

    /** Empty counts as 0; anything that is not a whole number gives -1. */
    private static long read(EditText e) {
        String s = e.getText().toString().trim();
        if (s.isEmpty()) return 0;
        return Fmt.parseAmount(s);
    }

    private static void toast(MainActivity a, String msg) {
        a.snack(msg);
    }

    /** Debts that still have a balance, focus debt first. */
    private static List<Data.Debt> openDebts(Data d) {
        List<Data.Debt> list = new ArrayList<>();
        Data.Debt focus = d.focusDebt();
        if (focus != null) list.add(focus);
        for (Data.Debt x : d.debts) if (x != focus && d.balance(x) > 0) list.add(x);
        return list;
    }

    private static List<String> debtLabels(Data d, List<Data.Debt> debts) {
        List<String> names = new ArrayList<>();
        for (Data.Debt x : debts) names.add(x.name + " (" + Fmt.dh(d.balance(x)) + ")");
        return names;
    }

    static void confirm(MainActivity a, String title, String message, String okLabel, Runnable onOk) {
        new MaterialAlertDialogBuilder(a)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(okLabel, (dlg, which) -> onOk.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------- first run ----------

    static void setup(MainActivity a) {
        LinearLayout f = Ui.form(a);
        f.addView(Ui.text(a, "A few numbers to start. You can change them later.", Ui.BODY_MEDIUM, Ui.ON_SURFACE_VARIANT));
        final EditText name = Ui.input(a, f, "What do you owe? (name, optional)", Ui.TEXT, null);
        final EditText owed = Ui.input(a, f, "How much do you owe now?", Ui.NUMBER, "dh");
        final EditText target = Ui.input(a, f, "Paid toward it each week", Ui.NUMBER, "dh");
        final EditText pay = Ui.input(a, f, "Roughly earned each week", Ui.NUMBER, "dh");

        Ui.showForm(a, "Set up", f, "Start", () -> {
            long o = read(owed), t = read(target), p = read(pay);
            if (o < 0 || t < 0 || p < 0) {
                toast(a, "Use whole numbers only");
                return false;
            }
            if (o > 0) {
                Data.Debt debt = new Data.Debt();
                debt.id = a.data.newId();
                String n = name.getText().toString().trim();
                debt.name = n.isEmpty() ? "Debt" : n;
                debt.start = o;
                a.data.debts.add(debt);
            }
            a.data.settings.weeklyDebtTarget = t;
            a.data.settings.weeklyPay = p;
            a.changed();
            return true;
        });
    }

    // ---------- got paid ----------

    static void gotPaid(MainActivity a) {
        final Data d = a.data;
        final LocalDate today = LocalDate.now();
        final ZoneId zone = ZoneId.systemDefault();
        final long paidWeek = d.sumThisWeek(Data.Entry.DEBT, today, zone);
        final long due = Budget.debtDueThisWeek(d.settings.weeklyDebtTarget, d.totalRemaining(), paidWeek);
        final long reserved = Budget.reserved(due, paidWeek);
        final List<Data.Debt> open = openDebts(d);

        LinearLayout f = Ui.form(a);
        final EditText amount = Ui.input(a, f, "Amount received", Ui.NUMBER, "dh");

        TextView from = Ui.text(a, "From", Ui.LABEL_LARGE, Ui.ON_SURFACE_VARIANT);
        from.setPadding(0, Ui.dp(a, 16), 0, Ui.dp(a, 8));
        f.addView(from);
        final String[] sources = {"Salary", "Commission", "Other"};
        final Ui.Segmented source = Ui.segmented(a, sources, 0);
        f.addView(source.group);

        final TextView preview = Ui.text(a, "", Ui.BODY_LARGE, Ui.ON_SURFACE);
        preview.setPadding(0, Ui.dp(a, 16), 0, 0);
        f.addView(preview);

        final MaterialCheckBox alsoLog = new MaterialCheckBox(a);
        final boolean canLog = !open.isEmpty() && reserved > 0;
        final List<String> pickLabels = open.isEmpty() ? new ArrayList<String>() : debtLabels(d, open);
        if (pickLabels.isEmpty()) pickLabels.add("-");
        final Ui.Dropdown debtPick = Ui.dropdown(a, "Debt", pickLabels, 0);
        if (canLog) {
            alsoLog.setChecked(true);
            f.addView(alsoLog);
            Ui.add(f, debtPick.layout, 4);
        }

        Ui.Watcher w = new Ui.Watcher() {
            @Override
            void changed() {
                long amt = Math.max(0, read(amount));
                long[] split = Budget.splitPay(amt, reserved);
                preview.setText("Set aside for debt: " + Fmt.dh(split[0])
                        + "\nFree to spend: " + Fmt.dh(split[1]));
                alsoLog.setText("Also log " + Fmt.dh(split[0]) + " as paid to debt now");
            }
        };
        amount.addTextChangedListener(w);
        w.changed();

        Ui.showForm(a, "Got paid", f, "Save", () -> {
            long amt = read(amount);
            if (amt <= 0) {
                toast(a, "Enter an amount");
                return false;
            }
            a.data.add(Data.Entry.PAY, amt, 0, "", sources[source.index()]);
            long toDebt = Budget.splitPay(amt, reserved)[0];
            if (canLog && alsoLog.isChecked() && toDebt > 0) {
                Data.Debt chosen = open.get(Math.max(0, Math.min(debtPick.index, open.size() - 1)));
                long pay = Math.min(toDebt, a.data.balance(chosen));
                if (pay > 0) a.data.add(Data.Entry.DEBT, pay, chosen.id, "", "");
            }
            a.changed();
            return true;
        });
    }

    // ---------- paid debt ----------

    static void paidDebt(MainActivity a) {
        final Data d = a.data;
        final List<Data.Debt> open = openDebts(d);
        if (open.isEmpty()) {
            toast(a, d.debts.isEmpty() ? "Add a debt first" : "Nothing left to pay");
            return;
        }
        LinearLayout f = Ui.form(a);
        final Ui.Dropdown pick = Ui.dropdown(a, "Which debt", debtLabels(d, open), 0);
        Ui.add(f, pick.layout, 4);
        final EditText amount = Ui.input(a, f, "Amount paid", Ui.NUMBER, "dh");
        long suggested = Math.min(d.settings.weeklyDebtTarget, d.balance(open.get(0)));
        if (suggested > 0) amount.setText(String.valueOf(suggested));

        Ui.showForm(a, "Paid debt", f, "Save", () -> {
            long amt = read(amount);
            if (amt <= 0) {
                toast(a, "Enter an amount");
                return false;
            }
            Data.Debt target = open.get(Math.max(0, Math.min(pick.index, open.size() - 1)));
            long left = a.data.balance(target);
            if (amt > left) {
                toast(a, "That is more than the " + Fmt.dh(left) + " left on this debt");
                return false;
            }
            a.data.add(Data.Entry.DEBT, amt, target.id, "", "");
            a.changed();
            return true;
        });
    }

    // ---------- spent ----------

    static void spent(MainActivity a) {
        LinearLayout f = Ui.form(a);
        final EditText amount = Ui.input(a, f, "Amount spent", Ui.NUMBER, "dh");
        final EditText note = Ui.input(a, f, "What for (optional)", Ui.TEXT, null);

        ChipGroup chips = new ChipGroup(a);
        String[] names = {"Food", "Transport", "Phone", "Other"};
        for (String n : names) {
            final String chosen = n;
            Chip chip = new Chip(a);
            chip.setText(n);
            chip.setOnClickListener(v -> note.setText(chosen));
            chips.addView(chip);
        }
        Ui.add(f, chips, 12);

        Ui.showForm(a, "Spent", f, "Save", () -> {
            long amt = read(amount);
            if (amt <= 0) {
                toast(a, "Enter an amount");
                return false;
            }
            a.data.add(Data.Entry.SPEND, amt, 0, note.getText().toString().trim(), "");
            a.changed();
            return true;
        });
    }

    // ---------- debts ----------

    /** Pass null to add a new debt. */
    static void debtForm(MainActivity a, final Data.Debt existing) {
        LinearLayout f = Ui.form(a);
        final EditText name = Ui.input(a, f, "Name", Ui.TEXT, null);
        if (existing != null) name.setText(existing.name);
        final EditText amount = Ui.input(a, f,
                existing == null ? "Amount you owe now" : "Original amount", Ui.NUMBER, "dh");
        if (existing != null) amount.setText(String.valueOf(existing.start));
        if (existing != null) {
            TextView note = Ui.text(a, "Payments you logged are subtracted from the original amount.",
                    Ui.BODY_SMALL, Ui.ON_SURFACE_VARIANT);
            Ui.add(f, note, 8);
        }

        Ui.showForm(a, existing == null ? "Add debt" : "Edit debt", f, "Save", () -> {
            long amt = read(amount);
            String n = name.getText().toString().trim();
            if (amt <= 0) {
                toast(a, "Enter how much you owe");
                return false;
            }
            if (n.isEmpty()) n = "Debt";
            if (existing == null) {
                Data.Debt x = new Data.Debt();
                x.id = a.data.newId();
                x.name = n;
                x.start = amt;
                a.data.debts.add(x);
            } else {
                existing.name = n;
                existing.start = amt;
            }
            a.changed();
            return true;
        });
    }

    static void deleteDebt(MainActivity a, final Data.Debt debt) {
        confirm(a, "Delete " + debt.name + "?",
                "This also removes the payments logged to it. This cannot be undone.", "Delete", () -> {
                    a.data.removeDebt(debt.id);
                    a.changed();
                });
    }

    // ---------- history ----------

    static void deleteEntry(MainActivity a, final Data.Entry e, String description) {
        confirm(a, "Delete this entry?", description, "Delete", () -> {
            a.data.entries.remove(e);
            a.changed();
        });
    }

    // ---------- backup ----------

    static void restore(MainActivity a) {
        LinearLayout f = Ui.form(a);
        f.addView(Ui.text(a, "Paste the backup text you saved earlier. It replaces everything in the app.",
                Ui.BODY_MEDIUM, Ui.ON_SURFACE_VARIANT));
        final EditText box = Ui.input(a, f, "Paste backup here", Ui.MULTILINE, null);

        Ui.showForm(a, "Restore backup", f, "Restore", () -> {
            final Data fresh;
            try {
                fresh = Store.fromJson(box.getText().toString());
            } catch (org.json.JSONException ex) {
                toast(a, "That is not a valid backup");
                return false;
            }
            confirm(a, "Replace everything?",
                    "The backup has " + fresh.debts.size() + " debt(s) and " + fresh.entries.size()
                            + " entries. What is in the app now will be lost.",
                    "Replace", () -> a.replaceData(fresh));
            return true;
        });
    }

    static void eraseAll(MainActivity a) {
        confirm(a, "Erase everything?",
                "All debts, entries and settings on this phone will be deleted. Save a backup first if you may need it.",
                "Erase", () -> a.replaceData(new Data()));
    }
}
