package com.techmare.paydebt;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.techmare.paydebt.core.Budget;
import com.techmare.paydebt.core.Fmt;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Every pop-up form in the app. */
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
        Toast.makeText(a, msg, Toast.LENGTH_SHORT).show();
    }

    private static TextView label(MainActivity a, String s) {
        TextView t = Ui.text(a, s, 13, Ui.MUTED, false);
        t.setPadding(0, Ui.dp(a, 12), 0, 0);
        return t;
    }

    private static Spinner spinner(MainActivity a, List<String> items) {
        Spinner sp = new Spinner(a);
        ArrayAdapter<String> ad = new ArrayAdapter<>(a, android.R.layout.simple_spinner_item, items);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        return sp;
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
        new AlertDialog.Builder(a)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(okLabel, (dlg, which) -> onOk.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------- first run ----------

    static void setup(MainActivity a) {
        LinearLayout f = Ui.form(a);
        f.addView(Ui.text(a, "A few numbers to start. You can change them later.", 14, Ui.MUTED, false));
        f.addView(label(a, "What do you owe? (name, optional)"));
        final EditText name = Ui.textField(a, "e.g. Loan");
        f.addView(name);
        f.addView(label(a, "How much do you owe now?"));
        final EditText owed = Ui.numberField(a, "0");
        f.addView(owed);
        f.addView(label(a, "How much will you pay toward it each week?"));
        final EditText target = Ui.numberField(a, "0");
        f.addView(target);
        f.addView(label(a, "Roughly how much do you earn each week?"));
        final EditText pay = Ui.numberField(a, "0");
        f.addView(pay);

        Ui.showForm(a, "Set up", f, "Start", () -> {
            long o = read(owed), t = read(target), p = read(pay);
            if (o < 0 || t < 0 || p < 0) {
                toast(a, "Use whole numbers only");
                return false;
            }
            if (o > 0) {
                Data.Debt debt = new Data.Debt();
                debt.id = a.data.newId();
                debt.name = name.getText().toString().trim().isEmpty() ? "Debt" : name.getText().toString().trim();
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
        f.addView(label(a, "Amount received"));
        final EditText amount = Ui.numberField(a, "0");
        f.addView(amount);

        f.addView(label(a, "From"));
        List<String> sources = new ArrayList<>();
        sources.add("Salary");
        sources.add("Commission");
        sources.add("Other");
        final Spinner source = spinner(a, sources);
        f.addView(source);

        final TextView preview = Ui.text(a, "", 14, Ui.TEXT, false);
        preview.setPadding(0, Ui.dp(a, 14), 0, 0);
        f.addView(preview);

        final CheckBox alsoLog = new CheckBox(a);
        final Spinner debtPick = spinner(a, debtLabels(d, open));
        if (!open.isEmpty() && reserved > 0) {
            alsoLog.setChecked(true);
            f.addView(alsoLog);
            f.addView(debtPick);
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
            String src = (String) source.getSelectedItem();
            a.data.add(Data.Entry.PAY, amt, 0, "", src);
            long toDebt = Budget.splitPay(amt, reserved)[0];
            if (alsoLog.isShown() && alsoLog.isChecked() && toDebt > 0 && !open.isEmpty()) {
                Data.Debt target = open.get(Math.max(0, debtPick.getSelectedItemPosition()));
                long pay = Math.min(toDebt, a.data.balance(target));
                if (pay > 0) a.data.add(Data.Entry.DEBT, pay, target.id, "", "");
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
        f.addView(label(a, "Which debt"));
        final Spinner pick = spinner(a, debtLabels(d, open));
        f.addView(pick);
        f.addView(label(a, "Amount paid"));
        final EditText amount = Ui.numberField(a, "0");
        long suggested = Math.min(d.settings.weeklyDebtTarget, d.balance(open.get(0)));
        if (suggested > 0) amount.setText(String.valueOf(suggested));
        f.addView(amount);

        Ui.showForm(a, "Paid debt", f, "Save", () -> {
            long amt = read(amount);
            if (amt <= 0) {
                toast(a, "Enter an amount");
                return false;
            }
            Data.Debt target = open.get(Math.max(0, pick.getSelectedItemPosition()));
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
        f.addView(label(a, "Amount spent"));
        final EditText amount = Ui.numberField(a, "0");
        f.addView(amount);
        f.addView(label(a, "What for (optional)"));
        final EditText note = Ui.textField(a, "e.g. Food");
        f.addView(note);

        LinearLayout chips = Ui.hbox(a);
        chips.setPadding(0, Ui.dp(a, 10), 0, 0);
        String[] names = {"Food", "Transport", "Phone", "Other"};
        for (String n : names) {
            final String chosen = n;
            chips.addView(Ui.chip(a, n, v -> note.setText(chosen)));
        }
        f.addView(chips);

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
        f.addView(label(a, "Name"));
        final EditText name = Ui.textField(a, "e.g. Loan");
        if (existing != null) name.setText(existing.name);
        f.addView(name);
        f.addView(label(a, existing == null ? "Amount you owe now" : "Original amount"));
        final EditText amount = Ui.numberField(a, "0");
        if (existing != null) amount.setText(String.valueOf(existing.start));
        f.addView(amount);
        if (existing != null) {
            f.addView(Ui.text(a, "Payments you logged are subtracted from the original amount.", 12, Ui.MUTED, false));
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
        f.addView(Ui.text(a, "Paste the backup text you saved earlier. It replaces everything in the app.", 14, Ui.MUTED, false));
        final EditText box = new EditText(a);
        box.setHint("Paste backup here");
        box.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        box.setMinLines(4);
        box.setMaxLines(8);
        f.addView(box);

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
